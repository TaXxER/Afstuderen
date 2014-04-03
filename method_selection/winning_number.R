setwd("C:/Git-data/Afstuderen/method_selection")
require(ggplot2)
require(plyr)

### MAP ###
#read data
map_data = read.csv("map_results.csv")

#data transformations
map_temp <- map_data[,c(2:(length(map_data)-2))]
map_temp <- apply(map_temp, 2, function(x) if(sum(!is.na(x))>1) {x}else{"ILLEGAL"} )
map_temp <- map_temp[map_temp!="ILLEGAL"]
map_temp <- data.frame(map_temp)

# summation and normalization operations
map_winnum <- apply(map_temp, 2, function(x) sapply(x, function(y) if(is.na(y)){NA}else{sum(x[!is.na(x)]<y)}))
map_winnum <- data.frame(map_winnum)
map_ideal_winnum <- apply(map_winnum, 2, function(x) sapply(x, function(y) if(is.na(y)){0}else{if(length(x[!is.na(x)]) == 0){0}else{max(x[!is.na(x)])}}))
map_summed_winnum <- apply(map_winnum, 1, function(x) sum(x,na.rm=TRUE))
map_summed_ideal_winnum <- apply(map_ideal_winnum, 1, function(x) sum(x,na.rm=TRUE))
map_norm_winnum <- map_summed_winnum/map_summed_ideal_winnum
map_nr_measurements <- apply(map_temp,1,function(x) sum(!is.na(x)))
map_output <- data.frame("Method" = map_data[,1], "Normalised_winning_number" = map_norm_winnum, "Nr_measurements"= map_nr_measurements)

# add ID's to output
map_output$id <- seq_len(nrow(map_output))
map_output <- map_output[, c(ncol(map_output), 1:(ncol(map_output)-1))]


### NDCG@3 ###
# read data
ndcg3_data = read.csv("ndcg3_results.csv")

# data transformations
ndcg3_temp <- ndcg3_data[,c(2:(length(ndcg3_data)-2))]
ndcg3_temp <- apply(ndcg3_temp, 2, function(x) if(sum(!is.na(x))>1) {x}else{"ILLEGAL"} )
ndcg3_temp <- ndcg3_temp[ndcg3_temp!="ILLEGAL"]
ndcg3_temp <- data.frame(ndcg3_temp)

# summation and normalization operations
ndcg3_winnum <- apply(ndcg3_temp, 2, function(x) sapply(x, function(y) if(is.na(y)){NA}else{sum(x[!is.na(x)]<y)}))
ndcg3_winnum <- data.frame(ndcg3_winnum)
ndcg3_ideal_winnum <- apply(ndcg3_winnum, 2, function(x) sapply(x, function(y) if(is.na(y)){0}else{if(length(x[!is.na(x)]) == 0){0}else{max(x[!is.na(x)])}}))
ndcg3_summed_winnum <- apply(ndcg3_winnum, 1, function(x) sum(x,na.rm=TRUE))
ndcg3_summed_ideal_winnum <- apply(ndcg3_ideal_winnum, 1, function(x) sum(x,na.rm=TRUE))
ndcg3_norm_winnum <- ndcg3_summed_winnum/ndcg3_summed_ideal_winnum
ndcg3_nr_measurements <- apply(ndcg3_temp,1,function(x) sum(!is.na(x)))
ndcg3_output <- data.frame("Method" = ndcg3_data[,1], "Normalised_winning_number" = ndcg3_norm_winnum, "Nr_measurements"= ndcg3_nr_measurements)


### NDCG@5 ###
# read data
ndcg5_data = read.csv("ndcg5_results.csv")

# data transformations
ndcg5_temp <- ndcg5_data[,c(2:(length(ndcg5_data)-2))]
ndcg5_temp <- apply(ndcg5_temp, 2, function(x) if(sum(!is.na(x))>1) {x}else{"ILLEGAL"} )
ndcg5_temp <- ndcg5_temp[ndcg5_temp!="ILLEGAL"]
ndcg5_temp <- data.frame(ndcg5_temp)

# summation and normalization operations
ndcg5_winnum <- apply(ndcg5_temp, 2, function(x) sapply(x, function(y) if(is.na(y)){NA}else{sum(x[!is.na(x)]<y)}))
ndcg5_winnum <- data.frame(ndcg5_winnum)
ndcg5_ideal_winnum <- apply(ndcg5_winnum, 2, function(x) sapply(x, function(y) if(is.na(y)){0}else{if(length(x[!is.na(x)]) == 0){0}else{max(x[!is.na(x)])}}))
ndcg5_summed_winnum <- apply(ndcg5_winnum, 1, function(x) sum(x,na.rm=TRUE))
ndcg5_summed_ideal_winnum <- apply(ndcg5_ideal_winnum, 1, function(x) sum(x,na.rm=TRUE))
ndcg5_norm_winnum <- ndcg5_summed_winnum/ndcg5_summed_ideal_winnum
ndcg5_nr_measurements <- apply(ndcg5_temp,1,function(x) sum(!is.na(x)))
ndcg5_output <- data.frame("Method" = ndcg5_data[,1], "Normalised_winning_number" = ndcg5_norm_winnum, "Nr_measurements"= ndcg5_nr_measurements)
              
# add ID's to output
ndcg5_output$id <- seq_len(nrow(ndcg5_output))
ndcg5_output <- ndcg5_output[, c(ncol(ndcg5_output), 1:(ncol(ndcg5_output)-1))]


### NDCG@10 ###
#read data
ndcg10_data = read.csv("ndcg10_results.csv")

# data transformations
ndcg10_temp <- ndcg10_data[,c(2:(length(ndcg10_data)-2))]
ndcg10_temp <- apply(ndcg10_temp, 2, function(x) if(sum(!is.na(x))>1) {x}else{"ILLEGAL"} )
ndcg10_temp <- ndcg10_temp[ndcg10_temp!="ILLEGAL"]
ndcg10_temp <- data.frame(ndcg10_temp)

ndcg10_winnum <- apply(ndcg10_temp, 2, function(x) sapply(x, function(y) if(is.na(y)){NA}else{sum(x[!is.na(x)]<y)}))
ndcg10_winnum <- data.frame(ndcg10_winnum)
ndcg10_ideal_winnum <- apply(ndcg10_winnum, 2, function(x) sapply(x, function(y) if(is.na(y)){0}else{if(length(x[!is.na(x)]) == 0){0}else{max(x[!is.na(x)])}}))
ndcg10_summed_winnum <- apply(ndcg10_winnum, 1, function(x) sum(x,na.rm=TRUE))
ndcg10_summed_ideal_winnum <- apply(ndcg10_ideal_winnum, 1, function(x) sum(x,na.rm=TRUE))
ndcg10_norm_winnum <- ndcg10_summed_winnum/ndcg10_summed_ideal_winnum
ndcg10_nr_measurements <- apply(ndcg10_temp,1,function(x) sum(!is.na(x)))
ndcg10_output <- data.frame("Method" = ndcg10_data[,1], "Normalised_winning_number" = ndcg10_norm_winnum, "Nr_measurements" = ndcg10_nr_measurements)

# add ID's to output
ndcg10_output$id <- seq_len(nrow(ndcg10_output))
ndcg10_output <- ndcg10_output[, c(ncol(ndcg10_output), 1:(ncol(ndcg10_output)-1))]

### COMBINED ###
combined_methods <- NULL
combined_methods <- c(combined_methods, as.character(map_data[,1]))
combined_methods <- c(combined_methods, as.character(ndcg3_data[,1]))
combined_methods <- c(combined_methods, as.character(ndcg5_data[,1]))
combined_methods <- c(combined_methods, as.character(ndcg10_data[,1]))

combined_data <- NULL
combined_data <- cbind(map_summed_winnum,map_summed_ideal_winnum)
combined_data <- rbind(combined_data, cbind(ndcg3_summed_winnum, ndcg3_summed_ideal_winnum))
combined_data <- rbind(combined_data, cbind(ndcg5_summed_winnum, ndcg5_summed_ideal_winnum))
combined_data <- rbind(combined_data, cbind(ndcg10_summed_winnum, ndcg10_summed_ideal_winnum))

combined <- cbind.data.frame(combined_methods,combined_data)
colnames(combined) <- c("Method", "Summed_winnum", "Summed_ideal_winnum")
combined_aggregate <- aggregate(cbind(Summed_winnum, Summed_ideal_winnum)~Method,combined,sum)
combined_aggregate$Norm_winnum <- combined_aggregate$Summed_winnum/combined_aggregate$Summed_ideal_winnum
combined_output <- cbind(combined_aggregate$Summed_ideal_winnum, combined_aggregate$Norm_winnum)

# add ID's to output
combined_output$id <- seq_len(nrow(combined_output))
combined_output <- combined_output[, c(ncol(combined_output), 1:(ncol(combined_output)-1))]

### PLOT ###
### ADJUST HERE ###
# plot graph
#ggplot(ndcg5_output, aes(Nr_measurements,Normalised_winning_number)) + geom_point(size=2) + 
#  geom_text(aes(label=id), hjust=-0.5, vjust=-0.5) + 
#  theme_grey(base_size=12, base_family="") + 
#  scale_x_continuous("# of datasets evaluated on", expand = c(0,0), breaks=c(0,2,4,6,8,10,12,14, 16)) +
#  scale_y_continuous("Normalised Winning Number", expand = c(0,0), breaks=c(0,0.2,0.4,0.6,0.8,1.0)) +
#  expand_limits(x = c(0,16.35), y = c(0,1.03))

ggplot(combined_aggregate, aes(Summed_ideal_winnum,Norm_winnum)) + geom_point(size=2) +  
#  geom_text(aes(label=Method)) +
  scale_x_continuous("Ideal Winning Number", expand = c(0,0)) + 
  scale_y_continuous("Normalized Winning Number", expand=c(0,0)) +
  expand_limits(x = c(0,1000), y = c(0,1.01)) +
  theme(axis.title.y = element_text(size = rel(1.8), angle = 90)) +
  theme(axis.title.x = element_text(size = rel(1.8), angle = 00))
