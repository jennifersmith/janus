(ns janus.test.janus-test
  (:import [java.io Closeable])
  (:require [janus]
            [janus.dsl :refer [service contract method url header should-have request response of-type]]
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
         '(service "simple JSON service"
                   (contract
                    :contract-foo
                    "http://localhost:8080/"
                    (request
                     (method :get)
                     (header "Content-Type" "application/json"))
                    (response
                     (body
                      (of-type :json)
                      (should-have :path "$.id" :of-type :number)
                      (should-have :path "$.features[*]" :matching #"[a-z]")))))
         (janus/unsafe-verify)
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
                  (of-type :json)
                  (should-have :path "$.features[*]" :matching #"[a-z]")))))
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
               (of-type :json)
               (should-have :path "$.origin"
                            :matching #"^[A-Z]{3,3}$")
               (should-have :path "$.destination"
                            :matching #"^[A-Z]{3,3}$")
               (should-have :path "$.departDate"
                            :matching #"^[0-9]{4,4}-[0-9]{2,2}-[0-9]{2,2}$")
               (should-have :path "$.itineraries" :of-type :number)))))
          (janus/unsafe-verify)
          (get-in [:results "POST valid search" :result]))
         => :succeeded)))
