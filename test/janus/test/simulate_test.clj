(ns janus.test.simulate-test
  (:require [midje.sweet :refer :all]
            [janus.dsl :refer :all]
            [janus.verify :refer [verify-service]]
            [janus.simulate :refer [simulate]]))




(def a-service   (service "Bob's weather service"
           (contract :weather-radar
                     "http://localhost:8787/cities"
                     (request
                      (method :get)
                      (header "content-type" "application/json"))
                     (response
                      (body
                       (content-type :json)
                       (should-have :path "$.image"
                                    :of-type :object
                                    (should-have :path "$.description" :of-type :string)
                                    (should-have :path "$.url" :of-type :string)))))))

(with-state-changes [(around :facts (with-open [sim (simulate a-service 8787)] ?form))]
        (future-fact "my client's contract is my service's simulation spec"
              (-> a-service 
                  (verify-service {})
                  (get-in [:results :weather-radar])) => (contains [[:result :succeeded]])))
