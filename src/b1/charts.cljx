(ns b1.charts
  (:require [b1.layout.histogram :as h]
            [b1.scale :as scale]
            [b1.svg :refer [translate scale line]]))

(defn histogram [xs & {:keys [bins x-axis y-axis]
                       :or {bins 20 x-axis true y-axis true}}]
  (let [hist (map meta (h/histogram xs :value identity
                                    :bins bins
                                    :range (constantly x-axis)))]
    {:histograms [hist]
     :bins bins
     :x-axis x-axis
     :y-axis y-axis
     :type :histogram}))

(defn add-histogram [{:keys [bins x-axis] :as histogram} xs]
  (update-in histogram [:histograms] conj
             (map meta (h/histogram xs :value identity
                                    :bins bins
                                    :range (constantly x-axis)))))

(defn function-area-plot [function & {:keys [x-axis y-axis]
                                      :or {x-axis true y-axis true}}]
  {:functions [function]
   :x-axis x-axis
   :y-axis y-axis
   :type :function-area})

(defn add-function [plot function]
  (update-in plot [:functions] conj function))
