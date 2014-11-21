setwd("C:/Git-data/Afstuderen/implementation/RankLib/")
library(ggplot2)
library(mgcv)
library(stringr)
library(scales)
measure = read.csv("normalization_measurements_LETOR3.csv") # read csv file
measure = measure[measure$dataset=='HP2003',]

d <- ggplot(data=measure, aes(x=iters, y=ndcg, colour=norm, shape=norm))
d <- d + geom_point(size=2) + 
	theme_bw() + 
	theme(
		panel.grid.major = element_blank(),
		panel.grid.minor = element_blank(),
		panel.border     = element_blank(),
		panel.background = element_blank(),
		axis.line = element_line(color='black')
	) +
	xlab("# of iterations") +
	ylab("NDCG@10 on test set") +
	labs(colour = "Execution mode", shape = "Execution mode") 
ggplot_build(d)