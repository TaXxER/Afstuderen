setwd("C:/Git-data/Afstuderen/measurements")
library(ggplot2)
library(mgcv)
library(scales)
measure = read.csv("raw_data.csv") # read csv file
name <- paste(measure$serial.parallel, measure$nodeCount, sep = " ")
name <- ifelse(name=="Serial NA", "Serial", name)
measure$serial.parallel <- name
maxTime <- max(measure$trainTime)
d <- ggplot(data=measure, aes(x=dataSize, y=trainTime, colour=serial.parallel))
d <- d + geom_point(size=2) + stat_smooth(se=FALSE, method = "loess", formula = y ~ exp(x)) + 
	scale_x_log10() + theme_bw() + 
	theme(
		panel.grid.major = element_blank(),
		panel.grid.minor = element_blank(),
		panel.border     = element_blank(),
		panel.background = element_blank(),
		axis.line = element_line(color='black')
	) + scale_y_continuous(expand = c(0.001, maxTime/100))
ggplot_build(d)