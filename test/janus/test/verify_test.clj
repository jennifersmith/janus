(ns janus.test.verify-test
  [:use janus.verify
   midje.sweet]
  [:require [clj-http.client :as http]])

(unfinished )

(fact
  (extract-clause :s ..contract.. ..context..) => [[:s 1] [:s 2]]
  (provided
    ..contract.. =contains=> {:clauses [[:s 1]]}
    ..context.. =contains=> {:clauses [[:s 2]]}))

(fact "a status clause should check against a response correctly"
  (check-clause {:status 200} [:status 201]) => "Expected status 201. Got status 200")

(fact "a header clause should allow equality and matching checks"
      (check-clause {:headers {"ct" "blah"}} [:header {:name "ct" :value "blah"}]) => empty?
      (check-clause {:headers {"ct" "foo"}} [:header {:name "ct" :value "bar"}]) => "Expected header 'ct' to equal 'bar'. Got 'foo'.")

(fact "reponse with different status or ct fails"
      
      (errors-in-envelope {:status 200} [[:status 201]] {}) => ["Expected status 201. Got status 200"]
      
      (errors-in-envelope {:headers {"ct" "app/json"}} [[:header {:name "ct" :value "text/html"}]] {})
      => ["Expected header 'ct' to equal 'text/html'. Got 'app/json'."])

(fact "the body is checked depending on body type spec"
  (errors-in-body {:body "ok"} [[:body [[:of-type :json] [:foo :bar]]]] {}) => ..json-validation..
  (provided (janus.json-response/verify-document "ok" [[:of-type :json] [:foo :bar]])
            => ..json-validation..))

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

;;;arrrrrgh
(against-background
  [(build-request ..request.. ..endpoint.. ..context..) => ..http-request..
   ..contract.. =contains=> {:name "sample contract" :endpoint ..endpoint.. :request ..request.. :response ..response..}
   ..failing-contract.. =contains=> {:name "sample contract" :endpoint ..endpoint.. :request ..request.. :response ..failing-response..}
   (http/request ..http-request..) => ..http-response..
   (validate-response ..response.. ..http-response.. ..context..) => []
   (validate-response ..failing-response.. ..http-response.. ..context..) => ["Expected status to be: 201. Got: 200"]]
  
  (fact "a valid service succeeds"  
    (verify-contract ..contract.. ..context..) => {:result :succeeded})

  (fact "a service with an invalid response provides descriptive messages"
        (verify-contract ..failing-contract.. ..context..) => {:result :failed
                                                               :errors ["Expected status to be: 201. Got: 200"]}))

(facts
 (verify-service {:name "svc" :contracts [{:name "c1"}]} ..context..) =>  {:service "svc" :results {"c1" ..res..}}
  (provided
   (verify-contract {:name "c1"} ..context..) => ..res..))
