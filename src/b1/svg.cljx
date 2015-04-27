(ns b1.svg
  (:require [b1.maths :refer [Pi Tau radians-per-degree
                              sin cos mean]]))

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
