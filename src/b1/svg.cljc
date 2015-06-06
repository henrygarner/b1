(ns b1.svg
  (:require [b1.layout.histogram :as h]
            [b1.maths :refer [Pi Tau radians-per-degree
                             sin cos mean]]
            [b1.scale :as scale]
            [b1.ticks :refer [search]]
            [clojure.string :as string]
            #?@(:cljs
                [[goog.string :as gstring]
                 [goog.string.format :as format]])))

(defn ->xy
  "Ensure that coordinates (potentially map of `{:x :y}`) are a seq or vector pair."
  [coordinates]
  (cond
   (and (sequential? coordinates) (= 2 (count coordinates))) coordinates
   (map? coordinates) [(:x coordinates) (:y coordinates)]))

(defn translate [coordinates]
  (let [[x y] (->xy coordinates)]
    (str "translate(" (float x) "," (float y) ")")))

(defn scale [coordinates]
  (if (number? coordinates)
    (str "scale(" (float coordinates) ")")
    (let [[x y] (->xy coordinates)]
      (str "scale(" (float x) "," (float y) ")"))))

(defn rotate
  ([angle] (rotate angle [0 0]))
  ([angle coordinates]
     (let [[x y] (->xy coordinates)]
       (str "rotate(" (float angle) "," (float x) "," (float y) ")"))))


(defn ^:cljs get-bounds
  "Returns map of `{:x :y :width :height}` containing SVG element bounding box.
   All coordinates are in userspace. Ref [SVG spec](http://www.w3.org/TR/SVG/types.html#InterfaceSVGLocatable)"
  [$svg-el]
  (let [b (.getBBox $svg-el)]
    {:x (.-x b)
     :y (.-y b)
     :width (.-width b)
     :height (.-height b)}))

(defn transform-to-center
  "Returns a transform string that will scale and center provided element `{:width :height :x :y}` within container `{:width :height}`."
  [element container]
  (let [{ew :width eh :height x :x y :y} element
        {w :width h :height} container
        s (min (/ h eh) (/ w ew))]
    (str (translate [(- (/ w 2) (* s (/ ew 2)))
                     (- (/ h 2) (* s (/ eh 2)))]);;translate scaled to center
         " " (scale s) ;;scale
         " " (translate [(- x) (- y)]) ;;translate to origin
         )))

(defn axis
  "Returns axis <g> hiccup vector for provided input `scale` and collection of `ticks` (numbers).
   Direction away from the data frame is defined to be positive; use negative margins and widths to render axis inside of data frame.
   Kwargs:
   > *:orientation* &in; (`:top`, `:bottom`, `:left`, `:right`), where the axis should be relative to the data frame, defaults to `:left`
   > *:formatter* fn run on tick values, defaults to `str`
   > *:major-tick-width* width of ticks (minor ticks not yet implemented), defaults to 6
   > *:text-margin* distance between axis and start of text, defaults to 9
   > *:label* axis label, centered on axis; :left and :right orientation labels are rotated by +/- pi/2, respectively
   > *:label-margin* distance between axis and label, defaults to 28"
  [scale ticks & {:keys [orientation
                         formatter
                         major-tick-width
                         text-margin
                         label
                         label-margin]
                  :or {orientation :left
                       formatter str
                       major-tick-width 6
                       text-margin 9
                       label-margin 28}}]

  (let [[x y x1 x2 y1 y2] (case orientation
                            (:left :right) [:x :y :x1 :x2 :y1 :y2]
                            (:top :bottom) [:y :x :y1 :y2 :x1 :x2])

        parity (case orientation
                 (:left :top) -1
                 (:right :bottom) 1)

        text-anchor (case orientation
                      :left "end"
                      :right "start"
                      (:top :bottom) "middle")
        
        text-baseline (case orientation
                        (:left :right) "middle"
                        :bottom "text-before-edge"
                        :top "text-after-edge")]

    [:g {:class (str "axis " (name orientation))}
     [:line.rule (apply hash-map (interleave [y1 y2] (:range scale)))]
     [:g.ticks
      (map (fn [d scale]
             [:g.tick.major-tick {:transform (translate {x 0 y (scale d)})}
              [:text {:text-anchor text-anchor
                      :transform (translate {x (* parity 8) y 0})
                      :style {:dominant-baseline text-baseline}}
               (formatter d)]
              [:line {x1 0 x2 (* parity major-tick-width)}]]) ticks (repeat scale))]

     (when label
       [:text.label {:transform (str (translate {x (* parity label-margin)
                                                 y (mean (:range scale))})
                                     " "
                                     (case orientation
                                       :left (rotate -90)
                                       :right (rotate 90)
                                       ""))}
        label])]))

(defn line
  "Return a Hiccup path SVG element with the [x,y] coordinates in the points sequence connected by lines"
  [points]
  (let [[[x y] & xs] points]
    [:path {:d (apply str "M" x "," y
                           (for [[x y] xs] (str "L" x "," y)))}]))

(def ArcMax (- Tau 0.0000001))

(defn circle
  "Calculate SVG path data for a circle of `radius` starting at 3 o'clock and sweeping in positive y."
  ([radius] (circle [0 0] radius))
  ([coordinates radius]
     (let [[x y] (->xy coordinates)]
       (str "M"  (+ x radius) "," y
            "A" (+ x radius) "," (+ y radius) " 0 1,1" (- (+ x radius)) "," y
            "A" (+ x radius) "," (+ y radius) " 0 1,1" (+ x radius) "," y))))

(defn arc
  "Calculate SVG path data for an arc."
  [& {:keys [inner-radius, outer-radius
             start-angle, end-angle, angle-offset]
      :or {inner-radius 0, outer-radius 1
           start-angle 0, end-angle Pi, angle-offset 0}}]
  (let [r0 inner-radius
        r1 outer-radius
        [a0 a1]  (sort [(+ angle-offset start-angle)
                        (+ angle-offset end-angle)])
        da (- a1 a0)
        large-arc-flag (if (< da Pi) "0" "1")

        s0 (sin a0), c0 (cos a0)
        s1 (sin a1), c1 (cos a1)]

    ;;SVG "A" parameters: (rx ry x-axis-rotation large-arc-flag sweep-flag x y)
    ;;see http://www.w3.org/TR/SVG/paths.html#PathData
    (if (>= da ArcMax)
      ;;Then just draw a full annulus
      (str "M0," r1
           "A" r1 "," r1 " 0 1,1 0," (- r1)
           "A" r1 "," r1 " 0 1,1 0," r1
           (if (not= 0 r0) ;;draw inner arc
             (str "M0," r0
                  "A" r0 "," r0 " 0 1,0 0," (- r0)
                  "A" r0 "," r0 " 0 1,0 0," r0))
           "Z")

      ;;Otherwise, draw the wedge
      (str "M" (* r1 c0) "," (* r1 s0)
           "A" r1 "," r1 " 0 " large-arc-flag ",1 " (* r1 c1) "," (* r1 s1)
           (if (not= 0 r0) ;;draw inner arc
             (str "L" (* r0 c1) "," (* r0 s1)
                  "A" r0 "," r0 " 0 " large-arc-flag ",0 " (* r0 c0) "," (* r0 s0))
             "L0,0")
           "Z"))))

(defmulti as-svg (fn [chart & args] (:type chart)))

(defmethod as-svg :histogram [{:keys [histograms  bins x-axis y-axis]
                               :or {x-axis true y-axis true}} & {:keys [width height]}]
  (let [margin-vertical (if x-axis 40 0)
        margin-horizontal (if y-axis 40 0)
        bars (->> histograms
                  (map-indexed (fn [i xs]
                                 (map #(assoc % :series i) xs)))
                  (apply concat))
        bar-width (/ (- width (* 2 margin-horizontal)) (count bars))
        scale-x (scale/linear :domain x-axis
                              :range [margin-horizontal
                                      (- width margin-horizontal)])
        max-y (->> bars
                   (map :y)
                   (apply max))
        scale-y (scale/linear :domain [0 max-y]
                              :range [(- height margin-vertical)
                                      margin-vertical])]
    [:svg {:width width :height height}
     [:g {:transform (translate [margin-horizontal 0])}
      (axis scale-y (:ticks (search [0 max-y])) :orientation :left)]
     [:g {:transform (translate [0 (- height margin-vertical)])}
      (axis scale-x (:ticks (search x-axis)) :orientation :bottom)]
     [:g.chart
      (for [bar bars]
        [:g.bar {:class (str "series-" (:series bar))
                 :transform (translate [(+ (-> bar :x scale-x)
                                           (* bar-width (:series bar)))
                                        (-> bar :y scale-y)])}
         [:rect {:height (->> bar :y scale-y (- height margin-vertical))
                 :width (dec bar-width)}]])]]))

(defmethod as-svg :function-area [{:keys [functions x-axis y-axis]} & {:keys [width height]}]
  (let [margin-vertical (if x-axis 40 0)
        margin-horizontal (if y-axis 40 0)
        scale-x (scale/linear :domain x-axis :range [margin-horizontal
                                                     (- width margin-horizontal)])
        y-vals (for [f functions]
                 (map (comp f (scale/invert scale-x)) (range width)))
        max-y (apply max (apply concat y-vals))
        scale-y (scale/linear :domain [0 (apply max (apply concat y-vals))]
                              :range [(- height margin-vertical)
                                      margin-vertical])
        points (for [val y-vals]
                 (concat [[margin-horizontal (- height margin-vertical)]]
                         (map vector (range width) (map scale-y val))
                         [[margin-horizontal (- height margin-vertical)]]))]
    [:svg {:width width :height height}
     [:g {:transform (translate [margin-horizontal 0])}
      (axis scale-y (:ticks (search [0 max-y] :length (- height
                                                         (* 2 margin-horizontal))))
            :formatter
            #?(:cljs 
               #(string/replace
                 (gstring/format "%.2f" %) #"\.?0+$" ""))
            #?(:clj
               #(string/replace
                 (format "%.2f" %) #"\.?0+$" ""))
            
            :orientation :left)]
     [:g {:transform (translate [0 (- height margin-vertical)])}
      (axis scale-x (:ticks (search x-axis)) :orientation :bottom)]
     [:g.chart
      (for [lines points]
        (line lines))]]))
