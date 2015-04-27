(ns b1.t-svg
  (:use midje.sweet
        [b1.svg :only [line]]))

(tabular
  (facts "Lines"
        (line ?coords) => ?expected)
  ?coords               ?expected
  [[0,0] [1,1] [5,5]]   [:path {:d "M0,0L1,1L5,5"}])
