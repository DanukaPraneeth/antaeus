## Antaeus

Antaeus (/ænˈtiːəs/), in Greek mythology, a giant of Libya, the son of the sea god Poseidon and the Earth goddess Gaia. He compelled all strangers who were passing through the country to wrestle with him. Whenever Antaeus touched the Earth (his mother), his strength was renewed, so that even if thrown to the ground, he was invincible. Heracles, in combat with him, discovered the source of his strength and, lifting him up from Earth, crushed him to death.

Welcome to our challenge.

## The challenge

As most "Software as a Service" (SaaS) companies, Pleo needs to charge a subscription fee every month. Our database contains a few invoices for the different markets in which we operate. Your task is to build the logic that will pay those invoices on the first of the month. While this may seem simple, there is space for some decisions to be taken and you will be expected to justify them.

### Structure
The code given is structured as follows. Feel free however to modify the structure to fit your needs.
```
├── pleo-antaeus-app
|
|       Packages containing the main() application. 
|       This is where all the dependencies are instantiated.
|
├── pleo-antaeus-core
|
|       This is where you will introduce most of your new code.
|       Pay attention to the PaymentProvider and BillingService class.
|
├── pleo-antaeus-data
|
|       Module interfacing with the database. Contains the models, mappings and access layer.
|
├── pleo-antaeus-models
|
|       Definition of models used throughout the application.
|
├── pleo-antaeus-rest
|
|        Entry point for REST API. This is where the routes are defined.
└──
```

## Instructions
Fork this repo with your solution. We want to see your progression through commits (don’t commit the entire solution in 1 step) and don't forget to create a README.md to explain your thought process.

Please let us know how long the challenge takes you. We're not looking for how speedy or lengthy you are. It's just really to give us a clearer idea of what you've produced in the time you decided to take. Feel free to go as big or as small as you want.

Happy hacking 😁!

## How to run
```
./docker-start.sh
```

## Libraries currently in use
* [Exposed](https://github.com/JetBrains/Exposed) - DSL for type-safe SQL
* [Javalin](https://javalin.io/) - Simple web framework (for REST)
* [kotlin-logging](https://github.com/MicroUtils/kotlin-logging) - Simple logging framework for Kotlin
* [JUnit 5](https://junit.org/junit5/) - Testing framework
* [Mockk](https://mockk.io/) - Mocking library


### Solution by [Danuka Praneeth](https://danukap.com) 


### Development Approaches

**Appraoch 1 :** This requirement can be achieved via a single long running batch process to select all the 'pending' invoices, make the payment and then to update the database. This approach is fairly straightforward and easy to implement. But I realised few limitations in this method. They are large processing time with the increase of batch size, overhead in the database (eg : db locks) in querying and updating a huge batch of data, possibility of rolling back the whole batch process with a single payment transaction failure, difficulties in scaling the batch process in distributed deployments etc.  


**Appraoch 2 :** So to overcome those drawbacks, approach 2 was developed to initialise a scheduled task for each 'pending' invoice using the Quartz job scheduler. This gives us more control for the execution of the payment schedules, to more efficiently handle the resource utilization. So now we can scale the 'pending' invoice payments over different durations of the day and over different nodes in the cluster. This will minimise the overhead on database queries, any ongoing processes in the server and also the overhead on 3rd party payment provider.

**Reasons to select Quartz**
* open source job scheduling library
* can be instantiated as a cluster of stand-alone programs with load-balance and fail-over capabilities
* availability of many online resources 

### Implementation

**Special Note :** Non of the existing modules or models were changed during the development of this solution since the information on any external components dependant on existing mudules/models were not available.  

#### Modifications to the Data Access Layer
- New method was added to this layer to update the existing invoices

#### Modifications to main application layer 
- Updated the initialisation of the billing service
- Since the data is inserted into the tables at the instantiation of the project, initialisation of the scheduled task for the billing service was done at this point. 
- But in the actual scenario this scheduled task initialisation can be done at the point of creating a new invoice entry to the tables at the invoice service class.

#### Modifications to Service layer
- BillingService class was modified to implement the logic of creation of the quatz scheduled job
- Here I have defined the cronschedule using a random variable to randomly distribute the scheduled task execution to different time periods of the day on 1st of every month. This will further to scale the batch process in a single server node
- BillServiceExecutor class was created to implement the logic to perform the invoice payment and to handle different error scenarios.
- Scheduled task will be destroyed if the invoice payment is successfully completed
  
### Further Improvements
- Initialising new scheduled tasks for future new invoices created in the system
- Test cases to cover full functionality of introduced new functions covering all possible error scenarios
- Implement the logic to handle false response at PaymentProvider service (customer account balance did not allow the charge). eg: Introduce new state for customer as 'suspended/blocked' and temporary stop the subscribed services 
- I didnt't see exception handling for database related transactions in the current implementation, so followed the same pattern for new invoice update method. But going forward exceptions related to database operations can be handled properly in the data layer and a proper custom exception should be thrown to the business layer

### External libraries

Latest stable version of Quartz job scheduling library (2.3.0)


<br />
It took me around 2.5 days to implement the logic and test cases. Additionally I spend around 2 days to learn the basics of Kotlin.

I will be happy to discuss any further details on this.