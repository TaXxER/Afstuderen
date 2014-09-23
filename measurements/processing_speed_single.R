setwd("C:/Git-data/Afstuderen/measurements")
library(ggplot2)
library(boot)
library(mgcv)
library(stringr)
library(scales)
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
measure$throughput <- measure$dataSize/measure$seconds
measure$serial.parallel <- name
d <- ggplot(data=measure, aes(x=dataSize, y=throughput, colour=serial.parallel, shape=serial.parallel))
d <- d + geom_point(size=2) + geom_line() + 
	theme_bw() + 
	theme(
		panel.grid.major = element_blank(),
		panel.grid.minor = element_blank(),
		panel.border     = element_blank(),
		panel.background = element_blank(),
		axis.line = element_line(color='black')
	) + scale_x_continuous(expand = c(0.01, 0)) + scale_y_continuous(expand = c(0, 0)) +
	xlab("Dataset Size (in Byte)") +
	ylab("Throughput (in Byte/Second)") +
	labs(colour = "Execution mode", shape = "Execution mode") 
ggplot_build(d)