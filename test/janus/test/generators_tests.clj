(ns janus.test.generators-tests
  (:require [midje.sweet :refer :all]
           [janus.generators :refer :all]
           [janus.dsl :refer [should-have body content-type]]))

(fact "Able to work back from pieces of the DSL to generate response for basic datatypes"
      (generate-data (should-have :path "$.name" :of-type :string)) 
      => (contains [[:name string?]])
      (generate-data (should-have :path "$.temp" :of-type :number))       
      => (contains [[:temp number?]]))

(fact "Able to combine two different matchers to create a composite" 
      (generate-data
       (body
        (should-have :path "$.name" :of-type :string)
        (should-have :path "$.temp" :of-type :number)))
      => (contains [[:name string?] [:temp number?]]))

;; TODO: Object here means 'array'... well in the example anyway. Need to fix the dsl.

(fact "Able to create nested object arrays" 
      (->
       (body
        (should-have :path "$.cities"
                     :of-type :object
                     (should-have :path "$.name" :of-type :string)
                     (should-have :path "$.temp" :of-type :number)))
       (generate-data)
       (:cities))
      => (some-checker empty? (contains (contains [[:name string?] [:temp number?]]))))


