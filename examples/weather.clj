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
;;      (wrap-trace :header :ui)
      ))

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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; weather readings
;;;;;;;;;;;;;;;;;;;;;;;;

(def city-data (atom 
                [{:name "Melbourne" 
                  :temp 16 
                  :outlook {
                            :today "Monsoon" 
                            :tomorrow "Heat wave"}}
                 {:name "London" 
                  :temp 7 
                  :outlook {
                            :today "Rain" 
                            :tomorrow "More bloody rain"}}
                 {:name "Chicago" 
                  :temp 3 
                  :outlook {
                            :today "High winds" 
                            :tomorrow "High winds"}}]))

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

;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Cities resource (liberator)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defresource cities
  :available-media-types ["application/json"]
  :handle-ok (fn [ctx] {:cities @city-data}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; start the weather service on port 8787
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn start-bobs-weather-service []
  (let [routes (routes (ANY "/cities" [] cities))]
    (-> routes
        (wrap-app)
        (serve* 8787 true))
    (browse-bobs-weather-service)
    (create-city-updater)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Define our consumer contract for Bob's weather service
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def bobs-weather-service-contract
  '(service 
   "Bob's weather service"
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Let's check the service against the contract
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn verify-bobs-weather-serivce []
  (pprint
   (verify/verify-service (eval bobs-weather-service-contract) {})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Clojurescript weather dashboard
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def browse-jens-weather-dashboard
  (partial browse-url "http://localhost:3000/"))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; If the service stops meeting the contract...
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn remove-names []
  (swap! city-data (partial map #(dissoc % :name))))

;;;;;;;;;;;;;;
;;; restore data
;;;;;;;;;;;;;;;

(defn restore-names []
  (swap! city-data (partial map #(assoc %2 :name %1) ["Melbourne" "London" "Chicago"] )))

;;;;;;;;;;;;;;;;;;;;
;; If the service gets some additive changes
;;;;;;;;;;;;;;;;;;;;
(defn add-outlook []
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Calling out to the live API might be impractical - maybe costs $$ or is unreliable.
;; As we know what the service needs we can simulate it
;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn simulate-bobs-weather-service []
  (simulate (eval bobs-weather-service-contract) :port 8787 :client-origin "http://localhost:3000")
  (browse-bobs-weather-service))

;;;;;;;;;;;;;;;
;; Maybe we need to rethink the contract and put in some constraints
;;;;;;;;;;;;;;;

(def bobs-weather-service-contract-with-constraints
  '(service 
   "Bob's more realistic weather service"
   (contract :city_list
             "http://localhost:8787/cities"
             (request
              (method :get)
              (header "content-type" "application/json"))
             (response
              (header "content-type" "application/json;charset=UTF-8")
              (body
               (content-type :json)
               (should-have :cities
                                  (of-type :collection)
                                  (should-have-name :name :of-type :string)
                                  (should-have-name :temp :of-type :number)))))))
