(ns janus.test.json-response-test
  (:require [clojure.data.json :as json]
            [janus.json-response :refer :all]
            [midje.sweet :refer :all]))

(unfinished )

(fact
  (extract-rule [:json-path "" :equal-to "b"]) => :equal-to)

(fact
  (check :equal-to :hello :hello) => empty?
  (check :equal-to :world :hello) => ["Expected \":world\". Got \":hello\""])

(fact
 (check :of-type :string "") => empty?
 (check :of-type :string :b) => ["Expected \":b\" to be string"] 
  (check :of-type :array [:a :b]) => empty?
  (check :of-type :array {:a :b} ) => ["Expected \"{:a :b}\" to be array"]
  (check :of-type :object {:a "val"}) => empty?
  (check :of-type :object "val") => ["Expected \"val\" to be object"]
  (check :of-type :number 10) => empty?
  (check :of-type :number 1.0) => empty?
  (check :of-type :number "") => ["Expected \"\" to be number"])

(fact
  (check :matching #"[a-z]+" "hello") => empty?
  (check :matching #"[a-z]+" "hello there") => ["Expected \"hello there\" to match regex [a-z]+"]
  (check :matching #"[a-z]+" "HELLO") => ["Expected \"HELLO\" to match regex [a-z]+"]
  (check :matching #"[a-z]" 10) => ["Expected \"10\" to match regex [a-z]"])

(fact
 (verify-clause {:foo "a"} [:json-path "$.foo" :matching #"[a-z]"]) => empty?
 (verify-clause {:foo "1"} [:json-path "$.foo" :matching #"[a-z]"]) => ["failed, at path $.foo"]
  (provided
   (check :matching #"[a-z]" anything) => ["failed"]))

(fact
 (verify-clause {:foo ["a"]} [:json-path "$.foo[*]" :matching #"[a-z]"]) => empty?
 (verify-clause {:foo ["1"]} [:json-path "$.foo[*]" :matching #"[a-z]"]) => ["failed, at path $.foo[*]"]
  (provided
   (check :matching #"[a-z]" anything) => ["failed"])
  (verify-clause {:foo []} [:json-path "$.foo[*]" :of-type :string]) => ["Nothing found at path $.foo[*]"])

(fact
  (verify-document "\"body\"" [[:json-path "$", :equal-to "body"]]) => empty?
  (verify-document "{\"foo\": [\"a\"]}" [[:json-path "$.foo[*]", :matching #"[a-z]"]]) => empty?
  (verify-document "{\"foo\": [\"1\"]}" [[:json-path "$.foo[*]", :matching #"[a-z]"]]) =not=> empty?)

(fact
 (verify-document (json/json-str {:foo [1,2,3]}) [[:json-path "$.foo" :of-type :number]] ) => empty?
  (verify-document (json/json-str {:foo [1,2,3]}) [[:json-path "$.foo" :of-type :string]] ) =not=> empty?)

(fact "we can nest expressions for more detailed testing of objects"
 (verify-document (json/json-str {:foo [{:a 1} {:a 2}]}) 
                  [[:json-path "$.foo" :of-type :object [[:json-path "$.a" :of-type :number]]]]) => empty?
 (verify-document (json/json-str {:foo [{:a 1} {:a 2}]}) 
                  [[:json-path "$.foo" :of-type :object 
                    [[:json-path "$.a" :of-type :string]]]] ) =not=> empty?) 

(fact "nested expressions don't work on things that are not expected to be objects"
 (verify-document (json/json-str {:foo [{:a 1} {:a 2}]}) 
                  [[:json-path "$.foo" :of-type :number
                    [[:json-path "$.a" :of-type :string]]]]) => (contains (contains "Contract error: :of-type :number cannot specify child clauses")))


(fact "bad json gives a good error" 
      (verify-document "baaad" {}) => ["Invalid json document: baaad"])
