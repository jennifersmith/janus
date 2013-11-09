(ns janus.test.generators-tests
  (:require [midje.sweet :refer :all]
           [janus.generators :refer :all]
           [janus.dsl :refer :all]))

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


(fact
 "can cope with should-haves"
 (generate-data (should-have :foo))
 => (contains [[:foo anything]]))

(fact
 "can cope with types"
 (generate-data (should-have :foo (of-type :number)))
 => (contains [[:foo number?]]))

(fact "combines multiple each clauses into one map"
      (take 4  (generate-data (each (should-have :foo (of-type :string))
                                    (should-have :bar (of-type :number))))) =>
                           (some-checker empty?
                                         (contains (contains [[:foo string?] [:bar number?]]))))

(fact "if body does not constrain, add stupidly high constraints and uniformly distribute"
      (count (generate-data  (each 
                              (of-type :number)))) =>  (roughly 0 65000))

(future-fact "constraints to count you ask for"
      (count (generate-data (each [0 100]
                             (of-type :number)))) => (roughly 0 100))

(future-fact "constrains to the range you ask for ")

