setwd("C:/Git-data/Afstuderen/measurements")
library(ggplot2)
library(mgcv)
library(stringr)
library(scales)
library(sitools)
measure = read.csv("raw_data.csv") # read csv file
name <- paste(measure$serial.parallel, measure$nodeCount, sep = " ")

getName <- function(s){
	return(ifelse(grepl("^Parallel (\\d)+$", s), 
			paste(
				c(
					"Cluster: ", 
					sprintf("% 3d",as.numeric(str_extract(s, "\\d+"))), 
					" data nodes"
				),
				collapse = ''
			), 
			"Serial"
		)
	)
}

name <- unlist(lapply(name, getName))
measure$seconds <- measure$trainTime/1000
measure$serial.parallel <- name
d <- ggplot(data=measure, aes(x=dataSize, y=seconds, colour=serial.parallel, shape=serial.parallel))
d <- d + geom_point(size=2) + geom_line() + 
	theme_bw() + 
	theme(
		panel.grid.major = element_blank(),
		panel.grid.minor = element_blank(),
		panel.border     = element_blank(),
		panel.background = element_blank(),
		axis.line = element_line(color='black')
	) + 
	scale_x_continuous(labels=f2si)+
	scale_y_continuous(expand = c(0, 0))+	
	xlab("Dataset size (in Byte)") +
	ylab("Training iteration time (in Seconds)") +
	labs(colour = "Execution mode", shape = "Execution mode") 
ggplot_build(d)