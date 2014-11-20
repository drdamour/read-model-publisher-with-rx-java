read-model-publisher-with-rx-java
=================================

Precursor
=======================
https://github.com/orfjackal/retrolambda - a bit more technical I'd read it first AND last

Nice step by step guide:
* http://blog.danlew.net/2014/09/15/grokking-rxjava-part-1/
* http://blog.danlew.net/2014/09/22/grokking-rxjava-part-2/
* http://blog.danlew.net/2014/09/30/grokking-rxjava-part-3/
* http://blog.danlew.net/2014/10/08/grokking-rxjava-part-4/


Presentation of Vision
======================================
Goal: Draw the Vision of Data Sharing

* App <-> API <-> Data (API not necessarily RESTful.  Take fullsite, UI is the site, API is ATG/droplets, etc, DB is Oracle)
* Show UX boundry.  UX is not the UI, it's what can be done, and how it's done.  Organization of data, methods available.  The heart of the application, where all the edge cases are solved.
* New App, different Ux, New Boundry on API: it's OK…but not always the best
* Split the API…still ok
* Build common API for common UX pieces….still ok
* Replicate the Data, move the UX completely, simplify the picture a bit
* Writes go back to original source, update a bunch of the lines with double arrows to single arrows
* How hypermedia helps with this, client doesn't need to know they're different "stacks"
* Bring in a 3rd replication, another DB, replicate some to 2nd writes still go to third.
* Erase like form 3rd to 2nd DB, make it 1st to third, make line read only from 3rd api to app, line from 3rd api to 2nd api.  ENDECA!


Attempts with Akka
=================================
* Draw Actor Model/Dependency Matrix
* Draw Oracle DB and Mongo DB
* Akka actors talking to oracle talking to mongo
* wrap oracle DB with Nucleus
* Describe Thread Pool and Akka's Approach to Millions of Actors
* Blocking IO vs Async IO
* Akka with multiple Thread pools, IO Actor Pool
* Multiple Thread Pools, Akka system inside ATG: note experienced enough
* What we lose: 
** 	recovery!
**	Living System
**	Built in monitoring tools
**	Channels that support multiple messages (Update Prod 123, Switch target to Database 2)


Positive outcome #1 from Akka: Ivy
=======================================
* Akka has a lot of dependencies and I'm lazy
* Ivy for dependency injection
* Command line app for DL dependencies…with a very nice Ant Task Plugin Set
** Ivy.xml
** Build.xml just needs
*** ivy-install.xml
*** ivy.xml
*** xmlns:ivy="antlib:org.apache.ivy.ant"
*** <import file="ivy-install.xml"/>
		<target name="resolve-libs" description="--> retrieve dependencies with ivy" depends="init-ivy">
		        <ivy:retrieve  />
		    </target>
** Works with our artifactory
** Show some tasks in intellij
* TODO: generate manifest.mf file


Positive outcome #2 from Akka: RxJava
=======================================
* Mention Talk about FRP
* Akka is inherently Asynchronous.  Talking ot Async stuff usually means Future, but a Future is just one response.  What about when many things can happen?  Rx is a nice solution.  https://github.com/ReactiveX/RxJava/wiki

* So you can use Rx to front asynchronous libraries: Akka, Retrofit, Hystrix, Etc
* You can use RxJava all by itself too.
* Refresher: Observable (out) and Observer(in).  Observer's subscribe to Observables.  When something is both an observable & an observer it is known as a Subject (remember for later!)
* Observables are composable via a lot of commong funciton concepts: map, join, flatMap, zip, merge, group, buffer, window, combine, switch, etc.

Changes the actor drawing a bit, instead of actors sending messages, it is Observers listening to Observables in the "System"

Intro
* 4 Test Files:
** AB_NO_DUPES
** AB_DUPES
** ABC_NO_DUPES
** ABC_DUPES
* Applications:simulateChanges
**  Reads line by line and sends them down
**  Simulates a collection of changes as part of a BCC project update.  In real implementation we'd take that BCC Project type instead of a file name
** Note the on completed
* Assets:
** Asset
**  BaseType
***    ID
***    Source
***    Creation Thread
**  TypeA
**  TypeB
**  TypeC
* Observers
**  "WriterObservers"
***	Just collect the things they see for inspection later
***	These would be the things that save stuff into a mongo DB
***	They write out all their contents on complete
**  LogAction just for various debugging output
  

Straight Project Update
========================
Test Class: Straight Through
Publish Subject -> A simple way to make an Observer
Func1 - a method that takes 1 parameter and returns something
Take the lines and split them on the : turn them into an Asset
Filter all assets to just TypeB's and TypeA's
Subscribe the observers
Simulate a project change

See output: Tppe id, source chain, thread created

Doing the split on : in all exaples would be a pain, so I wrote a helper AssetObservable.from(Observer, filename)

Also wrote AssetObserable.typeBs and typeAs to do the filtering
Same output

typeBs and typeAs works, but can also do as a generic filter using new filterType method

Which expands to 3 types nicely

Straight Project Update Dupes in Source
==================================
Test Class: StraightThroughDupes
Working with duplicate change requests is a big deal. BCC project might never have dupes, but our dependency graphs can create a lot of duplicates. IE if sku1 and sku2 are part of Product P, and sku 1 and sku 2 change, then 2 requests for Product P will be made.

New Hamcrest Matcher hasDuplicateInArray, by not() it we make sure there are no duplicates
First test fails

Introduce a distinct on the allChangeAssets..uses the equals on the Asset type.
Works on three types the same

B Depends on A
===========================
Test Class: BDependsOnA
bsTriggeredbyA - a simple map
tpyeBChange is a merge of all form source of type B and bsTriggeredByA
Also a new check that makes sure for every A there's a B
Since some A's have same ID as some B's in file…there are duplicate B's even though we distinct() the source of all..so that proves to not be the best solution

Instead we need to distinct the changes!

With a dupe source some of the distincts become unnecessary…distinct is not without overhead because it has to keep a collection of all previously seen things.  SO it is good to minimize them…but it can also great reduce downstream CPU and memroy because it can make the working set smaller…so it's the common CPU vs Memory trade off.

C Depends on A or B
============================
Test Class: CDependsOnAOrB
Very similar to B depends on A, but this time we have csTriggeredByA, csTriggeredByB and then merge all those in with the Cs from source.  Good example of composition
Fails because some B's and A's have the same id, so that leads to the same C's which means dupes

By distincting the outputs we once again avoid dupes

We then do it with the dupes file…but add a log action to show how many "requests" for changes were observed, compared to the actual final output you can see a lot of dupes came through

By distincting the source types, the number of things observed at the end has lessened greatly.  With such a small chain this isn't a big difference, but when we build big chains this could end up being a big deal

C Depends on B or A, B depends on A
=====================================
Test Class: CDependsOnAOrBBdependsOnA

Extra chain of dependencies in here.  Note that technically this could be implemented as C depends on B, B depends on A…I didn' tdo this logic optimiziation, but it could make sense as long as we knew all A's triggered B's that would trigger the same C's that the original A would.  This trite exaple does that…but this is unlikely in the real world.

First test fails becase dupes in B
If we distinct everything..it all works

Now we run against dupes file and also output # of changes observed vs actual.  The disparity has gotten even bigger (because all C see's all A's twice, once from A's and then once indirectly from B's

If we distinct they typeBChanged and typeA changed…the numbers of observed vs acutal output get a lot closer.


C Depends on A AND B
==================================
TestClass: CDependsOnAandB
Interesting case here, where we would only need to publish something if two things changed

You can't do this with the operators we've been using: filter, map, merge, distinct
Need a new one, called join!
Join is a cross product of everything form one observable cross with everything from another observable.  Just like a cross join in SQL
Then you just filter down to the results you care about (in our case where A.id == B.id)
And map those into C's
This is a LOT like SELECT A.id, B.id FROM A CROSS JOIN B WHERE A.id = B.id;
Old style joins…which are notoriously inefficient

There's a better operator for something like this GroupJoin!
Instead of calling your join function for every A or B.  It calls your function for every A it encounters passing in an observable of all the B's it will encounter.  You then build off this B observable to send out all the cases you care about.  In this case we only care about the B's that match the A, so we perform a filter on the B observable to find only those, and then for all of these we return a pair of A's and B's.
This is then mapped into a C.
Note that we could instead of returning the pairs, just returned the C.
This is exactly how non-indexed SQL STANDARD joins are implemented in DB's.  Ala: SELECT a.id, b.id FROM A LEFT JOIN B ON A.ID == B.ID

A depends on B, B depends on A
======================================
TestCase: BDependsOnAADependsOnB
A circular case.  This is very real world and something we need to avoid as it leads to endless loops.

The first naïve approach I had was just to make a bsTriggeredByA and a asTriggerByB and just merge them up to each other…but it was a real chiken and egg thing because the merge produced a new observable that is what I really wanted…this wasn't going to work

I needed a way to subscribe to any request changes of a type and yet have a single output of all requested changes.  
Using subjects (which are Observables AND Observers) to subscribe to all requested changes, but also be the publisher (observable) of all changes made the chicken and egg problem go away!

But this just exposed the next problem: infinite loops.


Started to think of Requested Changes & Verified Changes.  In and Out respectively.  I only want to write stuff based upon verified changes. Also I only want to generate dependent requests from Verified Changes.

Using subjects (which are Observables AND Observers) as the thing that takes incoming requested changes and outputs verified changes the chicken egg problem was gone, but it also allowed for me to deal with the infinite loop.
What causes the infinite loop, the fact that the same requests are being output as verified.  We haven't actually done any verification.  Verification can mean just about anything, but in these examples what really makes a request verified is that it hasn't been requested before…our old friend distinct()

Just because I had distinct on "verified" changes doesn't mean not having distinct in other places isn't still valuable…

B Depends On A When A is Odd
====================================
TestClass: BDependsOnAWhenAIsOdd
Up till now we've always had unconditional dependencies.  Anytime A changes B changes, etc.  This isn't real world.  There will be changes that only trigger changes in other things IF certain conditions are met.

We simulate this by making B's only appear if A's are odd.

At this point we introduce a ReadModelSystem which has baked into it the concept of requested changes and verified changes.  We extend thids base class add methods to perform various "dependency wiring" for our tests.

Making this conditionally dependent is a "bit" trickier because up till now every time we've subscribed to an observable (with a map) it's always has to output something.  So we've been using filter() to filter things out that we shouldn't care about prior to any map.  And this works.

Note we have a new test that makes sure for any odd a, a b exists, and for any even a a b does not exist.  (this is why the source files have the interesting constraint on them).

Filters work…but they are a bit inellegent in my opinon.  For one the map now depends on the filter being there, you can't remove one without serverly altering the other..that's pretty ugly.…and filter only works for reducing the possible set…not for expanding it (we'll see that in the next section).

To get the logic together we have to make it such that in a Map we may (when a is odd) or may not (when a is even) publish something that should be observed.  This means map actually returns an Observable<Observable<T>>..this is actualy rather common occurrence.  And manytimes when yousee this, you want to "flatten" this down to a single Observable<T>…and guess what…this is built in with the Observable.merge() call.

In fact the parttern of reacting to an event by publishing a whole observable only for all those observables to be merged'd (or flattened) is so common, there's a build in composition function for this known as flatMap()

Side note. Observeable.just creates an obesrable that publishes the single event and then completes.  It has overloads that take more than 1.

B Depends On A Two Bs Per A
================================
Test Class: BDependsOnATowBsPerA

Another real world case.  In this simulation for every A of id X we must update a B of id X+10 and a B of id X-10.

As mentioned before filter doesn't really work when you aren't reducing the set.  That's not totally true, you just have to have two separate filters…which is fine.  But what if it produces 3, or 4, or 100 changes.  Now it gets really ugly.  Also what if you won't know how many changes it should produce until you actually looka t the requested change?  You have to be a bit more reactive.

Just like checking when A is odd,  we have the same solutions for this.  First there's the dependency on merge, you use Map to return an Observable<Oberable<T>> which can fire off any number of dependencies.  In our case we used the overloaded JUST to return 2.  you then merge that Obervable of Obserbable into a single Observable.

But of course it's even easier to use flatMap

Multi Grabbing
====================
TestClass: MutiGrabbing

In some cases it may make sense to grab multiple changes and react to them in a group perhaps it makes less roundtrips to the persistence layer/db?

Buffer is the thing, turn an event T into a List<T>.  From there you have to get it back to an event of T…returning an Observable<T> and merging it down (or flatMap()) is a very nice way to do this.

Sometimes you don't want to just get them grouped in the order they came, but instead grouped with some kind of logic.  You can use groupBy to do this, then buffer those observables, then flapmap them back into a their own thing.

Multi Threaded Grabbing
=========================
TestClass: MultiThreadedGrabbing

In some cases it may make sense to do work on multiple threads RxJava has this baked in.

observeOn(Schedule) -> all things that observe this will be invoked on the given scheduler.. .The schedule the work of this actual observable  (like the map Func1 in a map()) will be done on remains unchanged.
subscribeOn(Schedule) -> the work that this observable actually does (like the map Func1 in a map()) is done on the given schedule..also all things that observe this are done on this schedule.

Put some observeOn's before some .maps to make them work on a different thread.

There's also Async Publishing Subjects to kick of things async

There's built in thread pools for compuation or io.  You can make other types if needed.


What's Missing?
=======================
Dealing with onError in RxJava
Determining everything is complete
Unsubscribing (may not be needed in Java) would just be a teardown in the System class
A/B switching -> there's techniques for this, just didn't get to them yet.  Key things:
	• when a Seed is happening…how do we buffer BCC projects to target B before we execute the siwtch
	• What do we do when a seed is in progress and another is requested
	• How do we reliably notify the API that it should switch db's?
	• How do we make sure a API server when it starts up uses the right db
Doing the actual writing…constructor chain followed by a writing subscriber I believe would be best
Writing only when needed…maybe there a way to keep a hash/etag in mongo DB so that we can just discard some requested changes because we know they won't be needed.
Composition performance improvements.  If A has a B, and B changed then when creating A we don't need to recreate B since B was already created by the B writer.  How do we send a handle to the updated B to this A?  Thinking Join's in the writer chains
Backpressure - cause we need real data
Retro lambda - https://github.com/orfjackal/retrolambda


