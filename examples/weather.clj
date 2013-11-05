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
        [ring.middleware.cors :refer [wrap-cors]]
        [clojure.data.generators :as generators]))

;; todo: data generators?
;; anwyay you get the idea
(def random (new java.util.Random))

(defn folded-normal-int [mean]
  (-> (.nextGaussian random)
      (Math/abs)
      (* mean)
      (Math/round)))

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

(def city-data (atom [{:name "Melbourne" :temp 16 :outlook {:today "Monsoon" :tomorrow "Heat wave"}}
                      {:name "London" :temp 7 :outlook {:today "Rain" :tomorrow "More bloody rain"}}
                      {:name "Chicago" :temp 3 :outlook {:today "High winds" :tomorrow "High winds"}}]))

(defn adjust-temp [current]

  (let [[up-weight down-weight maintain-weight]
        (case [(<= current -5) (> 0 current -5) (< 35 current 45) (>= current 45)]
          [true false false false] [100 0 0]
          [false true false false] [50 10 40]
          [false false true false] [10 50 40]
          [false false false true] [0 100 0]
          [25 25 50]
          )]
    (generators/weighted {current maintain-weight (inc current) up-weight (dec current) down-weight })))

 (defn adjust-temps [current]
     (map #(update-in %1 [:temp] adjust-temp )
          current))

(defn create-city-updater []
  (future
    (loop []
      (let [sleep-time (folded-normal-int 500)
            cities (map :cities city-data)]
        (swap! city-data adjust-temps )
        (Thread/sleep sleep-time)        
        (recur)))))

(defresource cities
  :available-media-types ["application/json"]
  :handle-ok {:cities (map #(select-keys % [:name :temp]) @city-data)})

(defresource city [id]
  :available-media-types ["application/json"]
  :exists? (fn [ctx]
             (if-let [city (first (filter #(= id (:name %)) @city-data))]
               {:city city}))
  :handle-ok (fn [ctx] (get ctx :city)))

(defn start-bobs-dodgy-service []
  (let [routes (routes (ANY "/cities" [] invalid-cities))]
    (-> routes
        (wrap-app)
        (serve* 8585 true))))

(defn start-bobs-weather-service []
  (let [routes (routes (ANY "/cities" [] cities)
                       (ANY "/city/:id" [id] (city id)))]
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
                       (matching-jsonpath "$.cities"
                                    :of-type :object
                                    (matching-jsonpath "$.name" :of-type :string)
                                    (matching-jsonpath "$.temp" :of-type :number)))))))


(defn verify-bobs-weather-serivce []
  (verify/verify-service bobs-weather-service-contract {}))

(defn simulate-bobs-weather-service []
  (simulate bobs-weather-service-contract :port 8787 :client-origin "http://localhost:3000"))

(def updater (atom nil))
(defn start-updater [] (swap! updater (constantly (create-city-updater))))
(defn kill-updater [] (swap! updater #(do (future-cancel %) nil)))

(defn main [& args]
  (start-updater)
  (start-bobs-weather-service)
  (println "Any key to quit")
  (read-line)
  (stop-server)
  (kill-updater))
