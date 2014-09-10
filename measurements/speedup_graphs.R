setwd("C:/Git-data/Afstuderen/measurements")
library(ggplot2)
library(mgcv)
library(stringr)
library(scales)
measure = read.csv("raw_data.csv") # read csv file
measure$seconds <- measure$trainTime/1000
measure$dataset <- factor(measure$dataset, levels = c("MINI", "OHSUMED", "MQ2008", "MQ2007", "MSLR-WEB10K", "MSLR-WEB30K", "CUSTOM-2", "CUSTOM-5", "CUSTOM-10", "CUSTOM-20", "CUSTOM-50", "CUSTOM-100"))
d <- ggplot(data=measure, aes(x=nodeCount, y=seconds))
d <- d + geom_point(size=2) + geom_line() + theme_bw() + 
	theme(
		panel.grid.major = element_blank(),
		panel.grid.minor = element_blank(),
		panel.border     = element_blank(),
		panel.background = element_blank(),
		axis.line = element_line(color='black')
	) + facet_wrap(~ dataset, scales="free")+
	xlab("Nodes in cluster") +
	ylab("Training iteration time (in seconds)")
ggplot_build(d)