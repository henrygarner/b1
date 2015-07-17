(ns b1.geo.projection
  #?(:clj (:require [b1.maths :refer [radians-per-degree sin cos sqrt]]
                    [b1.util :refer [c2-obj]]))
  #?(:cljs (:require [b1.maths :refer [radians-per-degree sin cos sqrt]]
                     [b1.util :refer-macros [c2-obj]])))

;; var width = 960,
;;     height = 1160;

;; UK
;; var projection = d3.geo.albers()
;;     .center([0, 55.4])
;;     .rotate([4.4, 0])
;;     .parallels([50, 60])
;;     .scale(6000)
;;     .translate([width / 2, height / 2]);

;;The [Albers](http://mathworld.wolfram.com/AlbersEqual-AreaConicProjection.html) equal-area conic projection
(c2-obj albers
        {:origin [-98 38]
         :parallels [29.5, 45.5]
         :scale 1000
         :translate [480 250]}

        #?(:clj clojure.lang.IFn :cljs IFn)
        (#?(:clj invoke :cljs -invoke)
           [this coordinates]
           (let [[lon lat] coordinates
                 phi1 (* radians-per-degree (first parallels))
                 phi2 (* radians-per-degree (second parallels))
                 lng0 (* radians-per-degree (first origin))
                 lat0 (* radians-per-degree (second origin))

                 s (sin phi1), c (cos phi1)
                 n (* 0.5 (+ s (sin phi2)))
                 C (+ (* c c) (* 2 n s))
                 p0 (/ (sqrt (- C (* 2 n (sin lat0)))) n)

                 t (* n (- (* radians-per-degree lon)
                           lng0))
                 p (/ (sqrt (- C (* 2 n (sin (* radians-per-degree lat)))))
                      n)]
             [(+ (* scale p (sin t)) (first translate))
              (+ (* scale (- (* p (cos t)) p0)) ;;Note that we've negated the p0 - p*cos(t) term so the projection is into a coordinate system where positive y is downward.
                 (second translate))])))
