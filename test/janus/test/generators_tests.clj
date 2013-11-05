(ns janus.test.generators-tests
  (:require [midje.sweet :refer :all]
           [janus.generators :refer :all]
           [janus.dsl :refer [matching-jsonpath body content-type]]))

(fact "Able to work back from pieces of the DSL to generate response for basic datatypes"
      (generate-data (matching-jsonpath "$.name" :of-type :string)) 
      => (contains [[:name string?]])
      (generate-data (matching-jsonpath "$.temp" :of-type :number))       
      => (contains [[:temp number?]]))

(fact "Able to combine two different matchers to create a composite" 
      (generate-data
       (body
        (matching-jsonpath "$.name" :of-type :string)
        (matching-jsonpath "$.temp" :of-type :number)))
      => (contains [[:name string?] [:temp number?]]))

;; TODO: Object here means 'array'... well in the example anyway. Need to fix the dsl.

(fact "Able to create nested object arrays" 
      (->
       (body
        (matching-jsonpath "$.cities"
                     :of-type :object
                     (matching-jsonpath "$.name" :of-type :string)
                     (matching-jsonpath "$.temp" :of-type :number)))
       (generate-data)
       (:cities))
      => (some-checker empty? (contains (contains [[:name string?] [:temp number?]]))))


