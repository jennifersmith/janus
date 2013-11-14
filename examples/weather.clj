(ns weather
  (:require 
   [clojure.java.browse :refer [browse-url]]
   [clojure.pprint :refer [pprint]]
   [weather-service]
   [janus.dsl :refer :all]
   [janus.verify :refer [verify-service]]
   [janus.simulate :refer [simulate]]))

;;==============================================
;; Start the service we want to integrate with
;; (weather-service/start)
;;==============================================


;;==============================================
;; Define the contract
;;==============================================
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
               (should-have :cities
                (each
                 (should-have :name (of-type :string))
   (should-have :temp (of-type :number)))))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Let's check the service against the contract
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn verify-bobs-weather-serivce []
  (dotimes [_ 2] (println))
  (pprint
   (verify-service bobs-weather-service-contract {})))
;; (verify-bobs-weather-serivce)


;;;;;;;;;;;;;;;;;
;; Now it meets the contract, let's think about consuming it to make a weather dashboard
;;;;;;;;;;;;;;;;;

(def browse-jens-weather-dashboard
  (partial browse-url "http://localhost:3000/"))

;; (weather-service/start false)
;; (browse-jens-weather-dashboard)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; If the service stops meeting the contract...
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  (weather-service/remove-names!)
  (weather-service/browse)
  (browse-jens-weather-dashboard)
  (verify-bobs-weather-serivce)
  (weather-service/reset-city-data!)
  (verify-bobs-weather-serivce)
;; what about something more fundamental like the content type
  (weather-service/start-html-only)
  (verify-bobs-weather-serivce)
  (weather-service/start false))

;;;;;;;;;;;;;;;;;;;;
  ;; If the service gets some additive changes
;;;;;;;;;;;;;;;;;;;;
(comment
  (weather-service/add-outlook!)
  (weather-service/browse)
  (verify-bobs-weather-serivce)
  (browse-jens-weather-dashboard))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Calling out to the live API might be impractical - maybe costs $$ or is unreliable.
;; As we know what the consumer needs we can simulate it
;;;;;;;;;;;;;;;;;;;;;;;;;;

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
               (should-have :cities
                (each
                 (should-have :name (of-type :string))
                 (should-have :temp (of-type :number)))))))))
(defn simulate-bobs-weather-service []
  (simulate bobs-weather-service-contract 
            :port 8787 
            :client-origin "http://localhost:3000")
  (weather-service/browse))

(comment
  (simulate-bobs-weather-service)
  (verify-bobs-weather-serivce)
  (browse-jens-weather-dashboard)) 


;; This is where a contract might start to get you to think about 
;; the edges of your system - the constraints

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
                ;;  NEW!
                (with-length-between 0 10)
                (each
                 (should-have :name 
                              (of-type :string) 
                              ;;  NEW!
                              (with-length-between 5, 20))
                 (should-have :temp 
                              (of-type :number)
                              ;;  NEW!
                              (with-range -5 45)))))))))

(defn verify-bobs-weather-serivce-with-constraints []
  (dotimes [_ 2] (println))
  (pprint
   (verify-service bobs-weather-service-contract-with-constraints {})))

(defn simulate-bobs-weather-service-with-constraints []
  (simulate bobs-weather-service-contract-with-constraints :port 8787 :client-origin "http://localhost:3000")
  (weather-service/browse))


(comment
  ;; First of all try and verify the real service
  (weather-service/start)
  (verify-bobs-weather-serivce-with-constraints)
  ;; now the simulation
  (simulate-bobs-weather-service-with-constraints)
  (verify-bobs-weather-serivce-with-constraints)
  ;; and now see if the dashboard looks better
  (browse-jens-weather-dashboard)
  ;; not bad. Obviously there is not any persistence in the cities on show. There is nothing in the contract that stipulates this - maybe there is a difference between a consumer contract and a "user contract"... that is what the user of the service expects.

  )

