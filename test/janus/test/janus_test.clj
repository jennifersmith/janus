(ns janus.test.janus-test
  (:import [java.io Closeable])
  (:require [janus]
            [janus.dsl :refer [service contract method url header should-have]]
            [midje.sweet :refer :all]

            [ring.util.serve :refer [serve* stop-server]]
            [ring.util.response :refer [response]]
            [ring.middleware.json :refer [wrap-json-response]]))



(defn create-handler [response-payload]
  (fn [request]
    (println request "HERE")
    response-payload))

(defn serve-response [response-payload]
  (serve* (wrap-json-response
          (create-handler response-payload))
          8080 true))

(defn serve-response-new [routes]
  (serve* (wrap-json-response routes)
          8080 true))


(against-background [(before :facts
                             (serve-response (response {:id 1 :features ["a" "b"]})))]
  (fact "Can verify a single contract for a running service"
    (janus/unsafe-verify
     '(service "simple JSON service"
               (contract
                "GET document"
                (method :get)
                (url "http://localhost:8080/")
                (header "Content-Type" "application/json")
                (should-have :path "$.id" :of-type :number)
                (should-have :path "$.features[*]" :matching #"[a-z]"))))
    => ["simple JSON service" :succeeded]))

(against-background
  [(before :facts
           (serve-response (response {:id 1 :features [10 "b"]})))]
  (fact "Can verify a single contract for a running service"
    (janus/unsafe-verify
     '(service "simple JSON service"
               (contract
                "GET document"
                (method :get)
                (url "http://localhost:8080/")
                (header "Content-Type" "application/json")
                (should-have :path "$.features[*]" :matching #"[a-z]"))))
    => ["simple JSON service"
        :failed
        [["GET document"
          :failed ["Expected \"10\" to match regex [a-z], at path $.features[*]"]]]]))

(against-background
  [(before :facts
           (serve-response-new
            (routes (POST "/" []
              {:origin "ORD"
               :destination "ATL"
               :paxCount 1
               :date "2012-03-21"}))
            ))]
  (fact "Should fail verification if we attempt a get on a post-only resource"
    (janus/unsafe-verify
     '(service
       "Shopping: One-way"
       (contract
        "GET valid search"
        (method :get)
        (url "http://localhost:4568/")
        (header "Content-Type" "application/json"))))
    => ["Shopping: One-way"
        :failed
        [["GET document"
          :failed ["Expected 200 status, got 501"]]]])

  (future-fact "Should verify that a post is possible"
    (janus/unsafe-verify
     '(service
       "Shopping: One-way"
       (contract
        "POST valid search"
        (method :post)
        (url "http://localhost:4568/search")
        (header "Content-Type" "application/json")
        (body :json
              {:origin "ORD"
               :destination "ATL"
               :paxCount 1
               :date "2012-03-21"})
        (should-have :path "$.origin"
                     :matching #"^[A-Z]{3,3}$")
        (should-have :path "$.destination"
                     :matching #"^[A-Z]{3,3}$")
        (should-have :path "$.departDate"
                     :matching #"^[0-9]{4,4}-[0-9]{2,2}-[0-9]{2,2}$")
        (should-have :path "$.itineraries" :of-type :number))))))
