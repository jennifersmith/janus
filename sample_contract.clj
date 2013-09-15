(service "simple JSON service"
      (contract "GET document"
        (method :get)
        (url "http://localhost:4568/service")
        (header "Content-Type" "application/json")

        (should-have :path "$.id" :of-type :number)
        (should-have :path "$.features[*]" :matching #"[a-z]")))

