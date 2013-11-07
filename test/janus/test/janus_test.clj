(ns janus.test.janus-test
  (:import [java.io Closeable])
  (:require [janus]
            [janus.verify :refer [verify-service]]
            [janus.dsl :refer :all]
            [liberator.core :refer [resource]]
            [midje.sweet :refer :all]
            [compojure.core :refer [routes ANY]]
            [ring.util.serve :refer [serve* stop-server]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]))

(defn serve-resource [resource]
  (let [routes (routes (ANY "/" [] resource))]
    (-> routes
        (wrap-json-response)
        (serve* 8080 true))))

(defn simple-get-resource [payload]
  (resource
   :allowed-methods [:get]
   :available-media-types ["application/json" "text/json"]
   :handle-ok (fn [ctx] payload)))

(defn post-only-resource [payload]
  (resource
   :available-media-types ["application/json" "text/json"]
   :allowed-methods [:post]
   :handle-created (fn [ctx] payload )))

(against-background
 [(before :facts
          (serve-resource
           (simple-get-resource
            {:id 1 :features ["a" "b"]})))]
  (fact "Can verify a single contract for a running service"
        (->
         (service "simple JSON service"
                   (contract
                    :contract-foo
                    "http://localhost:8080/"
                    (request
                     (method :get)
                     (header "Content-Type" "application/json"))
                    (response
                     (body                      
                      (content-type :json)
                      (should-have :id (of-type :number))
                      (should-have :features (of-type :collection)
                                   (with-length-between 2 8)
                                   (each (should-match #"[a-z]")))))))
         (verify-service {})
         (get-in [:results :contract-foo]))
        => (contains [[:result :succeeded]])))

(against-background
  [(before :facts
           (serve-resource (simple-get-resource {:id 1 :features [10 "b"]})))]
  (fact "Can verify a single contract for a running service"
    (->
     '(service "simple JSON service"
               (contract
                :c1
                "http://localhost:8080/"
                (request
                 (method :get)
                 (header "Content-Type" "application/json"))
                (response
                 (body
                  (content-type :json)
                  (matching-jsonpath "$.features[*]" :matching #"[a-z]")))))
     (janus/unsafe-verify)
     (get-in [:results :c1 :errors]))
    => (contains  "Expected \"10\" to match regex [a-z], at path $.features[*]")))

(against-background
  [(before :facts
           (serve-resource
            (post-only-resource
             {:status :awesome})))]
  (fact "Should fail verification if we attempt a get on a post-only resource"
        (->
         '(service
            "flight search"
            (contract
             :search
             "http://localhost:8080/"
             (request
              (method :get))
             (response
              (header "Content-Type" "application/json")
              (status 200))))
         (janus/unsafe-verify)
         (get-in [:results :search :errors]))
    => (contains "Expected status 200. Got status 405"))

(against-background
   [(before :facts
            (serve-resource
             (post-only-resource {:origin "ORD"
                                         :destination "ATL"
                                         :departDate "2014-01-01"
                                         :itineraries 44})))]

   (fact "Should verify that a post is possible"
         (->
          '(service
            "Search"
            (contract
             "POST valid search"
             "http://localhost:8080/"
             (request
              (header "Content-Type" "application/json")
              (method :post)
              (body :json
                    {:something "not validated"}))
             (response
              (body
               (content-type :json)
               (matching-jsonpath "$.origin"
                            :matching #"^[A-Z]{3,3}$")
               (matching-jsonpath "$.destination"
                            :matching #"^[A-Z]{3,3}$")
               (matching-jsonpath "$.departDate"
                            :matching #"^[0-9]{4,4}-[0-9]{2,2}-[0-9]{2,2}$")
               (matching-jsonpath "$.itineraries" :of-type :number)))))
          (janus/unsafe-verify)
          (get-in [:results "POST valid search" :result]))
         => :succeeded)))

(fact "Should error well if the service is not running"
  (->
   '(service "simple JSON service"
             (contract
              :contract-foo
              "http://localhost:11591/"
              (request
               (method :get)
               (header "Content-Type" "application/json"))
              (response
               (body
                (content-type :json)))))
   (janus/unsafe-verify)
   (get-in [:results :contract-foo]))
  => (contains [[:result :failed] 
                [:errors [{:class org.apache.http.conn.HttpHostConnectException, :message "Connection to http://localhost:11591 refused"}]]]))
