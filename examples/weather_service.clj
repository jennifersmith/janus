(ns weather-service
  (:require 
        [compojure.core :refer [routes ANY]]
        [ring.util.serve :refer [serve* stop-server]]
        [liberator.core :refer [defresource]]
        [liberator.dev :refer :all]
        [ring.middleware.content-type :refer [wrap-content-type]]
        [ring.middleware.cors :refer [wrap-cors]]
        [ring.util.response :refer [get-header]]
        [clojure.data.generators :as generators]
        [clojure.java.browse :refer [browse-url]]))

(def browse
  (partial browse-url "http://localhost:8787/cities"))

(defn- wrap-app [routes]
  (-> routes
      (wrap-content-type)
      (wrap-cors :access-control-allow-origin #"http://localhost:3000")
      (wrap-trace :header :ui)))

(defn- adjust-temp [current]
  (let [[up-weight down-weight maintain-weight]
        (case [(<= current -5) (> 0 current -5) (< 35 current 45) (>= current 45)]
          [true false false false] [100 0 0]
          [false true false false] [50 10 40]
          [false false true false] [10 50 40]
          [false false false true] [0 100 0]
          [25 25 50]
          )]
    (generators/weighted {current maintain-weight (inc current) up-weight (dec current) down-weight})))

 (defn- adjust-temps [current]
     (map #(update-in %1 [:temp] adjust-temp )
          current))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; weather readings
;;;;;;;;;;;;;;;;;;;;;;;;

(def default-cities [{:name "Melbourne" 
                   :temp 29 }
                  {:name "London" 
                   :temp 7 }
                  {:name "Washington DC" 
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

(def  city-updater (atom nil))

(defn- create-city-updater []
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
(defn start ([] (start true)) ([show-browser]
                  (let [routes (routes (ANY "/cities" [] cities))]
                    (-> routes
                        (wrap-app)
                        (serve* 8787 true))
                    (when show-browser
                      (browse))
                    (reset-city-data!)
                    (create-city-updater))))

(defn start-html-only []
  (let [routes (routes (ANY "/cities" [] cities-with-html))]
    (-> routes
        (wrap-app)
        (serve* 8787 true))
    (browse)))

