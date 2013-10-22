(ns janus.test.janus-test
  (:import [java.io Closeable])
  (:require [janus]
            [janus.dsl :refer [service contract method url header should-have]]
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
           (serve-resource (simple-get-resource {:id 1 :features [10 "b"]})))]
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
           (serve-resource
            (post-only-resource
             {:status :awesome})))]
  (fact "Should fail verification if we attempt a get on a post-only resource"
    (janus/unsafe-verify
     '(service
       "valid search"
       (contract
        "GET valid search"
        (method :get)
        (url "http://localhost:8080/")
        (header "Content-Type" "application/json")
        (should-have :status 200))))
    => ["valid search"
        :failed
        [["GET valid search"
          :failed ["Expected status 200. Got status 405"]]]])

(against-background
   [(before :facts
            (serve-resource
             (post-only-resource {:origin "ORD"
                                         :destination "ATL"
                                         :departDate "2014-01-01"
                                         :itineraries 44})))]

   (fact "Should verify that a post is possible"
         (janus/unsafe-verify
          '(service
            "Search"
            (contract
             "POST valid search"
             (method :post)
             (url "http://localhost:8080/")
             (header "Content-Type" "application/json")
             (body :json
                   {:something "not validated"})
             (should-have :path "$.origin"
                          :matching #"^[A-Z]{3,3}$")
             (should-have :path "$.destination"
                          :matching #"^[A-Z]{3,3}$")
             (should-have :path "$.departDate"
                          :matching #"^[0-9]{4,4}-[0-9]{2,2}-[0-9]{2,2}$")
             (should-have :path "$.itineraries" :of-type :number))))
         => ["Search" :succeeded])))
