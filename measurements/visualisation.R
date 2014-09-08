setwd("C:/Git-data/Afstuderen/measurements")
library(ggplot2)
library(scales)
measure = read.csv("raw_data.csv") # read csv file
name <- paste(measure$serial.parallel, measure$nodeCount, sep = " ")
name <- ifelse(name=="Serial NA", "Serial", name)
measure$serial.parallel <- name
d <- ggplot(data=measure, aes(x=dataSize, y=trainTime, colour=serial.parallel))
d <- d + geom_point() + geom_smooth()
d