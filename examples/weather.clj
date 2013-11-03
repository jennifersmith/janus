(ns weather
  (:require 
        [janus.dsl :refer :all]
        [janus.verify :as verify]
        [janus.simulate :refer [simulate]]
        [compojure.core :refer [routes ANY]]
        [ring.util.serve :refer [serve* stop-server]]
        [liberator.core :refer [defresource]]
        [liberator.dev :refer :all]
        [ring.middleware.content-type :refer [wrap-content-type]]
        [ring.middleware.cors :refer [wrap-cors]]))

(defn wrap-app [routes]
  (-> routes
      (wrap-content-type)
      (wrap-cors :access-control-allow-origin #"http://localhost:3000")
      (wrap-trace :header :ui)))

(defresource invalid-cities
  :available-media-types ["application/json"]
  :handle-ok {:cities
              [{:name "Melbourne"}
               {:name "London" }
               {:name "Chicago"}]})

(defresource cities
  :available-media-types ["application/json"]
  :handle-ok {:cities
              [{:name "Melbourne" :temp "22 ºC"}
               {:name "London" :temp "15 ºC"}
               {:name "Chicago" :temp "12 ºC"}]
              })

(defn start-bobs-dodgy-service []
  (let [routes (routes (ANY "/cities" [] invalid-cities))]
    (-> routes
        (wrap-app)
        (serve* 8585 true))))

(defn start-bobs-weather-service []
  (let [routes (routes (ANY "/cities" [] cities))]
    (-> routes
        (wrap-app)
        (serve* 8787 true))))


(def bobs-weather-service-contract
  (service "Bob's weather service"
           (contract :city_list
                     "http://localhost:8787/cities"
                     (request
                      (method :get)
                      (header "content-type" "application/json"))
                     (response
                      (header "content-type" "application/json;charset=UTF-8")
                      (body
                       (content-type :json)
                       (should-have :path "$.cities"
                                    :of-type :object
                                    (should-have :path "$.name" :matching #"\w+")
                                    (should-have :path "$.temp" :matching #"\d+ ºC")))))))


(defn verify-bobs-weather-serivce []
  (verify/verify-service bobs-weather-service-contract {}))

(defn simulate-bobs-weather-service []
  (simulate bobs-weather-service-contract 8787))
