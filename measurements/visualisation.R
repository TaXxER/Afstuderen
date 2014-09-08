setwd("C:/Git-data/Afstuderen/measurements")
library(ggplot2)
library(scales)
measure = read.csv("raw_data.csv") # read csv file
name <- paste(measure$serial.parallel, measure$nodeCount, sep = " ")
name <- ifelse(name=="Serial NA", "Serial", name)
measure$serial.parallel <- name
d <- ggplot(data=measure, aes(x=dataSize, y=trainTime, colour=serial.parallel))
d <- d + geom_point() + geom_smooth(fill=NA) + 
	scale_x_log10() + theme_bw() + opts(
		panel.grid.major = theme_blank(),
		panel.grid.minor = theme_blank(),
		panel.border     = theme_blank(),
		panel.background = theme_blank()
	)
d