(ns janus.test.simulate-test
  (:require [midje.sweet :refer :all]
            [janus.dsl :refer :all]
            [janus.verify :refer [verify-service]]
            [janus.simulate :refer [simulate]]))




(def a-contract   (service "Bob's weather service"
           (contract :city_list
                     "http://localhost:8787/cities"
                     (request
                      (method :get)
                      (header "content-type" "application/json"))
                     (response
                      (header "content-type" "application/json;charset=UTF-8")
                      (body
                       (of-type :json)
                       (should-have :path "$.cities"
                                    :of-type :object
                                    (should-have :path "$.name" :matching #"\w+")
                                    (should-have :path "$.temp" :matching #"\d+ ºC")))))))

(with-state-changes [(around :facts (with-open [sim (simulate a-contract 8787)] ?form))]
        (fact "my client's contract is my service's simulation spec"
              (-> a-contract 
                  (verify-service {})
                  (get-in [:results :city_list])) => (contains [[:result :succeeded]]) ))
