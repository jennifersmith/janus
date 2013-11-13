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
        [ring.util.response :refer [get-header]]
        [clojure.data.generators :as generators]
        [clojure.java.browse :refer [browse-url]]
        [clojure.pprint :refer [pprint]]))

(def browse-bobs-weather-service 
  (partial browse-url "http://localhost:8787/cities"))
;; todo: data generators?
;; anwyay you get the idea
(def random (new java.util.Random))

(defn wrap-app [routes]
  (-> routes
      (wrap-content-type)
      (wrap-cors :access-control-allow-origin #"http://localhost:3000")
      (wrap-trace :header :ui)))

(defn adjust-temp [current]
  (let [[up-weight down-weight maintain-weight]
        (case [(<= current -5) (> 0 current -5) (< 35 current 45) (>= current 45)]
          [true false false false] [100 0 0]
          [false true false false] [50 10 40]
          [false false true false] [10 50 40]
          [false false false true] [0 100 0]
          [25 25 50]
          )]
    (generators/weighted {current maintain-weight (inc current) up-weight (dec current) down-weight})))

 (defn adjust-temps [current]
     (map #(update-in %1 [:temp] adjust-temp )
          current))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; weather readings
;;;;;;;;;;;;;;;;;;;;;;;;

(def default-cities [{:name "Melbourne" 
                   :temp 16 }
                  {:name "London" 
                   :temp 7 }
                  {:name "Chicago" 
                   :temp 3}])

(def city-data (atom default-cities))


(defn reset-city-data! []
  (swap! city-data (constantly default-cities)))

(defn add-outlook! []
  (swap! city-data (partial map #(assoc %2 :outlook %1)
                            [{
                              :today "Monsoon" 
                              :tomorrow "Heat wave"} 
                             {
                              :today "Rain" 
                              :tomorrow "More bloody rain"}
                             {
                              :today "High winds" 
                              :tomorrow "High winds"}])))

(defn remove-names! []
  (swap! city-data (partial map #(dissoc % :name))))

;; (pprint city-data)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; simulate reading the weather
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def  city-updater (atom nil))

(defn create-city-updater []
  (if @city-updater
    (future-cancel @city-updater))
  (swap! city-updater
         (fn [_]
           (future
             (loop []
               (let [sleep-time 1000]
                 (swap! city-data adjust-temps )
                 (Thread/sleep sleep-time)        
                 (recur)))))))

(defresource cities
  :available-media-types ["application/json"]
  :handle-ok (fn [ctx] {:cities @city-data}))

(defresource cities-with-html
  :available-media-types ["text/html"]
  :handle-ok (fn [ctx] 
               @city-data))
(defn start-bobs-weather-service 
  ([] (start-bobs-weather-service true)) 
  ([browse]
     (let [routes (routes (ANY "/cities" [] cities))]
       (-> routes
           (wrap-app)
           (serve* 8787 true))
       (when browse
         (browse-bobs-weather-service))
       (reset-city-data!)
       (create-city-updater))))

(defn start-bobs-weather-service-html-only []
  (let [routes (routes (ANY "/cities" [] cities-with-html))]
    (-> routes
        (wrap-app)
        (serve* 8787 true))
    (browse-bobs-weather-service)))

;; START DEMO!

;;(start-bobs-weather-service)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def bobs-weather-service-contract
  (service 
   "Bob's weather service"
   (contract :city_list
             "http://localhost:8787/cities"
             (request (method :get))
             (response
              (status 200)
              (header "content-type" "application/json" )
              (json-body
               (should-have
                :cities
                (each
                 (should-have :name (of-type :string))
                 (should-have :temp (of-type :number)))))))))

;; the format is a bit of a mess but is a good data representation of our contract
;; (pprint bobs-weather-service-contract)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Let's check the service against the contract
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn verify-bobs-weather-serivce []
  (pprint
   (verify/verify-service bobs-weather-service-contract {})))
;; (verify-bobs-weather-serivce)

;;;;;;;;;;;;;;;;;
;; Now it meets the contract, let's think about consuming it to make a weather dashboard
;;;;;;;;;;;;;;;;;

(def browse-jens-weather-dashboard
  (partial browse-url "http://localhost:3000/"))

;; (browse-jens-weather-dashboard)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; If the service stops meeting the contract...
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


;; (remove-names!)
;; (browse-bobs-weather-service)
;; (browse-jens-weather-dashboard)
;; (verify-bobs-weather-serivce)
;; (reset-city-data!)
;; (verify-bobs-weather-serivce)
;; (start-bobs-weather-service-html-only)
;; (verify-bobs-weather-serivce)

;;;;;;;;;;;;;;;;;;;;
;; If the service gets some additive changes
;;;;;;;;;;;;;;;;;;;;

;;(add-outlook!)
;; (browse-bobs-weather-service)
;; (verify-bobs-weather-serivce)
;;(browse-bobs-weather-service)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Clojurescript weather dashboard
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def browse-jens-weather-dashboard
  (partial browse-url "http://localhost:3000/"))

;; (browse-jens-weather-dashboard)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Calling out to the live API might be impractical - maybe costs $$ or is unreliable.
;; As we know what the service needs we can simulate it
;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn simulate-bobs-weather-service []
  (simulate bobs-weather-service-contract :port 8787 :client-origin "http://localhost:3000")
  (browse-bobs-weather-service))


;; (simulate-bobs-weather-service)
;; (verify-bobs-weather-serivce)
;; (browse-jens-weather-dashboard) 


;; Add some constraints

(def bobs-weather-service-contract-with-constraints
  (service 
   "Bob's weather service"
   (contract :city_list
             "http://localhost:8787/cities"
             (request (method :get))
             (response
              (status 200)
              (header "content-type" "application/json" )
              (json-body
               (should-have
                :cities
                (with-length-between 0 10)
                (each
                 (should-have :name 
                              (of-type :string) 
                              (with-length-between 5 20))
                 (should-have :temp 
                              (of-type :number)
                              (with-range -5 45)))))))))

(defn verify-bobs-weather-serivce-with-constraints []
  (pprint
   (verify/verify-service bobs-weather-service-contract-with-constraints {})))

(defn simulate-bobs-weather-service-with-constraints []
  (simulate bobs-weather-service-contract-with-constraints :port 8787 :client-origin "http://localhost:3000")
  (browse-bobs-weather-service))


;; (verify-bobs-weather-serivce-with-constraints)
