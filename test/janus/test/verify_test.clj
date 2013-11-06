(ns janus.test.verify-test
  [:use janus.verify
   midje.sweet]
  [:require [clj-http.client :as http]])

(unfinished )

(fact "a header clause should allow equality and matching checks"
      (check-clause {:headers {"ct" "blah"}} [:header {:name "ct" :value "blah"}]) => empty?
      (check-clause {:headers {"ct" "foo"}} [:header {:name "ct" :value "bar"}]) => ["Expected header 'ct' to equal 'bar'. Got 'foo'."])

(fact "reponse with different status or ct fails"
      
      (validate-response [[:status 201]]  {:result :ok :response {:status 200}}  {}) => ["Expected status 201. Got status 200"]
      
      (validate-response  [[:header {:name "ct" :value "text/html"}]] {:result :ok :response {:headers {"ct" "app/json"}}} {})
      => ["Expected header 'ct' to equal 'text/html'. Got 'app/json'."])

(fact "the body is checked depending on body type spec"
      (validate-response  [[:body [[:content-type :json] [:foo :bar]]]] {:result :ok :response {:body "ok"}}  {})
  => [..json-validation..]
  (provided (janus.json-response/verify-document "ok" [[:content-type :json] [:foo :bar]])
            => [..json-validation..]))

(against-background [..contract.. =contains=> {:properties []}
                     ..context.. =contains=> {:properties []}]
  (fact "property values are extracted from the contract"
    (property "prop" ..contract.. ..context..) => "contract val"
    (provided
      ..contract.. =contains=> {:properties [{:name "prop" :value "contract val"}]})
    
    (property "prop" ..contract.. ..context..) => "first"
    (provided
      ..contract.. =contains=> {:properties [{:name "prop" :value "first"}
                                             {:name "prop" :value "second"}]})

    (property "prop" ..contract.. ..context..) => nil)

  (fact "property values are extracted from the context"
    (property "prop" ..contract.. ..context..) => "context val"
    (provided
      ..context.. =contains=> {:properties [{:name "prop" :value "context val"}]})))


(fact
  (to-xml [:tag]) => (contains "<tag></tag>")
  (to-xml [:tag {:attr "value"}]) => (contains "<tag attr=\"value\">")
  (to-xml [:tag [:sub]]) => (contains "<tag><sub></sub>")
  (to-xml [:tag "blah"]) => (contains "<tag>blah</tag>")
  (to-xml [:tag "blah" [:sub {:attr "val"}]]) => (contains "<tag>blah<sub attr=\"val\"></sub></tag"))

(fact
  (body-from ..contract.. {}) => "data"
  (provided
    ..contract.. =contains=> {:body {:type :string :data "data"}})
  (body-from ..contract.. {}) => "[\"a\",\"b\",{\"c\":\"hello\"}]"
  (provided
    ..contract.. =contains=> {:body {:type :json :data ["a", "b", {"c" "hello"}]}})
  (body-from ..contract.. {}) => (contains "<tag attr=\"value\"></tag>")
  (provided
    ..contract.. =contains=> {:body {:type :xml :data [:tag {:attr "value"}]}}))

(facts
 (verify-service {:name "svc" :entry-point {:name "c1" :url "http://foo/"} :contracts [{:name "c1"}]}) =>  {:service "svc" :results {"c1" ..res..}}
  (provided
   (verify-contract {:name "c1"} "http://foo/") => ..res..))
