# Dear Reviewer!  
I have had a lot of fun implementing this task  
It took me more time than I expected, so I needed to speed-up at some point and make some simplifications  
I started with doing full TDD (for cinema-schedule module), but then I almost entirely skipped testing for other modules.  
Because of that you should treat my solution like an outline, that presents my approach, but is not production ready.  
I would like to describe all the shortcuts I took, to let you know what was my idea to a full solution of the problem.  
But first, I will give you a short explanation of modules' responsibilities in my code.

## Modules
### cinema-schedule
Domain model for cinema-schedule aggregate.
I have decided to implement cinema-schedule as an event-sourced aggregate.  
For me the whole schedule for a cinema is one aggregate.  
I did not break it into separate aggregates for rooms,  
as I strongly believe the requirements of not having cleaning slots at the same time in different rooms,  
requires all schedule modifications to be applied sequentially.  
Handling concurrency would be not a responsibility of this module,  
but it is implemented in a way that suggest, that each schedule modification is a critical section,  
which can't be run in parallel with other modifications.

### cinema-schedule-app-service
- accepts users' commands and queries
- runs injected user input validators
- gets current cinema-schedule state from event-store
- adds configuration specific values to user input
- tries to apply requested schedule modifications or answer user queries
- in case of modifications, adds new cinema-schedule events to the event store

This can be thought of as the use-case layer

### movies-catalog
minimalistic module for Movie entity CRUD operations

### app
That's a module playing a role of a composition root  
It collects all the dependencies and injects them to cinema-schedule-app-service  
This way, my use case layer can abstract on particular implementations of it's ports (like event store)

## Things that I omitted 
In order to have a prod-ready solution, a lot of aspects would need to be added.  
Most important ones are:
 - real events store implementation (could be JDBC based or other, but some support for unique constraints would be needed)
 - roles and access control, tracking which Planner altered what
 - concurrency locks - more on it below
 - event-sourced aggregate snapshots, and archivisation of stale events - with time this could become a problem, let me know if you want me to describe what approach would I take for resolving it
## Approach to concurrency
As mentioned before, schedule modifications need to be applied sequentially.  
Movie catalog operations can go in parallel to schedule modifications, but I did not focus on it.

My default approach to concurrency control would be - to use optimistic locking.
In my case it could be implemented with a unique constraint on event version in the event store's persist storage.  

If many concurrent modification attempts are expected, some pessimistic locking would need to be added as well. 
It could be implemented with lock (either on some storage based records or some in memory data grid).  
Without obtaining such lock, modification request would not be proceeded.


