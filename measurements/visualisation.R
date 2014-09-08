setwd("C:/Git-data/Afstuderen/measurements")
library(ggplot2)
library(scales)
source("http://egret.psychol.cam.ac.uk/statistics/R/extensions/rnc_ggplot2_border_themes.r")
measure = read.csv("raw_data.csv") # read csv file
name <- paste(measure$serial.parallel, measure$nodeCount, sep = " ")
name <- ifelse(name=="Serial NA", "Serial", name)
measure$serial.parallel <- name
d <- ggplot(data=measure, aes(x=dataSize, y=trainTime, colour=serial.parallel))
d <- d + geom_point(size=2) + stat_smooth(se=FALSE) + 
	scale_x_log10() + theme_bw() + 
	theme(
		panel.grid.major = element_blank(),
		panel.grid.minor = element_blank(),
		panel.border     = element_blank(),
		panel.background = element_blank(),
		axis.line = element_line(color='black')
	)
d