(ns janus.test.janus-test
  (:import [java.io Closeable])
  (:require [janus]
            [janus.dsl :refer [service contract method url header should-have]]
            [midje.sweet :refer :all]
            [ring.util.serve :refer [serve* stop-server]]
            [ring.util.response :refer [response]]
            [ring.middleware.json :refer [wrap-json-response]]))



(defn handler [request]
  (response {:foo "bar"}))

(def app
  (wrap-json-response handler))

(defn create-handler [response-body]
  (fn [request]
    (response response-body)))

(defn serve-response [response-body]
  (serve* (wrap-json-response
          (create-handler response-body))
          8080 true))


(against-background [(before :facts
                             (serve-response {:id "1" :features ["a" "b"]}))]
  (facts "Can verify a single contract for a running service"
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

(against-background [(before :facts
                             (serve-response {:id 1 :features [10 "b"]}))]
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
