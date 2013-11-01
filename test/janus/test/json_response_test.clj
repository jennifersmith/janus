(ns janus.test.json-response-test
  (:require [clojure.data.json :as json]
            [janus.json-response :refer :all]
            [midje.sweet :refer :all]))

(unfinished )

(fact
  (extract-rule [:path "" :equal-to "b"]) => :equal-to)

(fact
  (equal-to :hello :hello) => empty?
  (equal-to :world :hello) => ["Expected \":world\". Got \":hello\""])

(fact
 (of-type :string "") => empty?
 (of-type :string :b) => ["Expected \":b\" to be string"] 
  (of-type :array [:a :b]) => empty?
  (of-type :array {:a :b} ) => ["Expected \"{:a :b}\" to be array"]
  (of-type :object {:a "val"}) => empty?
  (of-type :object "val") => ["Expected \"val\" to be object"]
  (of-type :number 10) => empty?
  (of-type :number 1.0) => empty?
  (of-type :number "") => ["Expected \"\" to be number"])

(fact
  (matching #"[a-z]+" "hello") => empty?
  (matching #"[a-z]+" "HELLO") => ["Expected \"HELLO\" to match regex [a-z]+"]
  (matching #"[a-z]" 10) => ["Expected \"10\" to match regex [a-z]"])

(fact
 (verify-clause {:foo "a"} [:path "$.foo" :matching #"[a-z]"]) => empty?
 (verify-clause {:foo "1"} [:path "$.foo" :matching #"[a-z]"]) => ["failed, at path $.foo"]
  (provided
   (matching #"[a-z]" anything) => ["failed"]))

(fact
 (verify-clause {:foo ["a"]} [:path "$.foo[*]" :matching #"[a-z]"]) => empty?
 (verify-clause {:foo ["1"]} [:path "$.foo[*]" :matching #"[a-z]"]) => ["failed, at path $.foo[*]"]
  (provided
   (matching #"[a-z]" anything) => ["failed"])
  (verify-clause {:foo []} [:path "$.foo[*]" :of-type :string]) => ["Nothing found at path $.foo[*]"])

(fact
  (verify-document "\"body\"" [[:path "$", :equal-to "body"]]) => empty?
  (verify-document "{\"foo\": [\"a\"]}" [[:path "$.foo[*]", :matching #"[a-z]"]]) => empty?
  (verify-document "{\"foo\": [\"1\"]}" [[:path "$.foo[*]", :matching #"[a-z]"]]) =not=> empty?)

(fact
 (verify-document (json/json-str {:foo [1,2,3]}) [[:path "$.foo" :of-type :number]] ) => empty?
  (verify-document (json/json-str {:foo [1,2,3]}) [[:path "$.foo" :of-type :string]] ) =not=> empty?)

(fact "we can nest expressions for more detailed testing of objects"
 (verify-document (json/json-str {:foo [{:a 1} {:a 2}]}) 
                  [[:path "$.foo" :of-type :object 
                    [:path "$.a" :of-type :number]]]) => empty?
 (verify-document (json/json-str {:foo [{:a 1} {:a 2}]}) 
                  [[:path "$.foo" :of-type :object 
                    [:path "$.a" :of-type :string]]] ) => empty?) ;; todo revisit after changing caller to expect back an array

