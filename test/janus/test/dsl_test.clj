(ns janus.test.dsl-test
  [:use [janus.dsl]]
  [:require [midje.sweet :as midje]])

(midje/unfinished)

(midje/fact "matching-jsonpath creates clauses as required"
            (matching-jsonpath "path" :of-type :type) => [:json-path "path" :of-type :type]
            (matching-jsonpath "foo" :of-type :object 
                         (matching-jsonpath "bar" :matching ...amazing-regex...)) => 
                         [:json-path "foo" :of-type :object 
                          [[:json-path "bar" :matching ...amazing-regex...]]])

(midje/fact "url creates a property containing the path"
            (url "path") => [:property {:name "url" :value "path"}])

(midje/fact "method creates a property containing the method"
            (method :meth) => [:method :meth])

(midje/fact "status creates a property containing the status"
            (status 105) => [:status 105])

;; overloading 'body' in req and resp... will be dodgy 
(midje/fact "body creates a body object with a type and data or matching clauses"
            (body (content-type :json) (equal-to {:sample "obj"}))
            => [:body [[:content-type :json] [:equal-to {:sample "obj"}]]]
            (body (matching-jsonpath "foo" :equal-to "foo")) 
            => [:body [[:json-path "foo" :equal-to "foo"]]])


(midje/fact "header creates a header"
            (header "Name" "Value") => [:header {:name "Name" :value "Value"}])

(midje/fact "request creates a request with matchers inside"
            (request (header "Name" "Value")) => [[:header {:name "Name" :value "Value"}]])

(midje/fact "respons creates a response with matchers inside"
            (response (header "Name" "Value")) => [[:header {:name "Name" :value "Value"}]])


(midje/fact "defining a contract as a request and response pairing"
            (contract "sample" (request) (response)) => {:name "sample" :request [] :response []})

(midje/fact "defining a contract with an endpoint"
            (contract "sample" "/uri" (request) (response)) => {:name "sample" :endpoint "/uri" :request [] :response []})

(midje/fact "defining a service as a cluster of contracts with one start point"
            (service "sample" 
                     (entry-point "star trek: first contract" "http://example.com/foo")
                     (contract "star trek: first contract" (request) (response))
                     (contract "second contract" (request) (response))) => 
                     { :name "sample"
                      :entry-point {:url "http://example.com/foo" 
                                    :name "star trek: first contract"}
                      :contracts [{:name "star trek: first contract", 
                                   :request [], :response []} 
                                  {:name "second contract", :request [], :response []}]} )

(midje/fact "loading a DSL program"
            (construct-domain '(service "sample" [])) => {:name "sample", :entry-point [] :contracts []})


