(ns weather
  (:require 
        [janus.dsl :refer :all]
        [janus.verify :as verify]
        [compojure.core :refer [routes ANY]]
        [ring.util.serve :refer [serve* stop-server]]
        [liberator.core :refer [defresource]]
        [liberator.dev :refer :all]
        [ring.middleware.content-type :refer [wrap-content-type]]))

(defresource cities
  :available-media-types ["application/json"]
  :handle-ok {:cities
              [{:name "Melbourne" :temp "22 ºC"}
               {:name "London" :temp "15 ºC"}
               {:name "Chicago" :temp "12 ºC"}]
              })


(defn start-bobs-weather-service []
  (let [routes (routes (ANY "/cities" [] cities))]
    (-> routes
        (wrap-content-type)
        (wrap-trace :header :ui)
        (serve* 8787 true))))


(def bobs-weather-service-contract
  (service "simple JSON service"
           (contract "List of cities"
                     (method :get)
                     (url "http://localhost:8787/cities")
                     (header "Content-Type" "application/json")
                     (should-have :path "$.cities"
                                  :of-type :object
                                  (should-have :path "$.name" :matching #"\w+")
                                  (should-have :path "$.temp" :matching #"\d+ ºC")))))


(defn verify-bobs-weather-serivce []
  (verify/verify-service bobs-weather-service-contract {}))
