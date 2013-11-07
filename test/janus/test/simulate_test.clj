(ns janus.test.simulate-test
  (:require [midje.sweet :refer :all]
            [janus.dsl :refer :all]
            [janus.verify :refer [verify-service]]
            [janus.simulate :refer [simulate]]
            [clj-http.client :as http]))


(def a-service   (service "Bob's weather service"
           (contract :weather-radar
                     "http://localhost:2020/cities"
                     (request
                      (method :get)
                      (header "content-type" "application/json"))
                     (response
                      (header "foo" "bar")
                      (body
                       (content-type :json)
                       (should-have :images
                                    (each
                                     (should-have :description (of-type :string))
                                     (should-have :url (of-type :string)))))))))


(with-state-changes [(around :facts 
                             (with-open [sim (simulate a-service 
                                                       :port 2020 
                                                       :client-origin "http://localhost:8080")] 
                               ?form))]
        (fact "my client's contract is my service's simulation spec"
              (-> a-service 
                  (verify-service {})
                  (get-in [:results :weather-radar]))
              => (contains [[:result :succeeded]]))
        
        (fact "a cors policy is added so I can access from the client origin"
              (:headers
               (http/get "http://localhost:2020/cities" 
                         {:headers {"origin" "http://localhost:8080"}}))
              => (contains [["access-control-allow-origin" "http://localhost:8080"]])))
