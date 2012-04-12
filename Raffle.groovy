@Grab( 'org.codehaus.groovy.modules.http-builder:http-builder:0.5.2' )
import groovyx.net.http.*
import groovy.json.JsonSlurper

// let's look past this for now...we'll circle back round to the secret sauce in a bit
ArrayList.metaClass.getRand = { number ->
    (0..<(number?:1)).collect{delegate[new Random().nextInt(delegate.size())]}
}

// and we need some way to recursively apply the secret sauce and assure no duplicate winners, no?
def getWinner
getWinner = { entryList ->
    def winner = entryList.getRand().member.name
    // no repeat winners, cheaters!
    entryList.remove(winner)/*Not sure if this will work, but if we can simply remove the winner from the list...
     Assuming we don't need to keep track of the winners, only the people who are left, it's a runtime improvement.
     This implementation avoids a failed guess: the situation that occurs  when we pick a random number applicable 
     to a previous winner and have to recurse again. It will keep our runtime consistent rather allowing the possibility of infinite 
     runtime. (Which happens as the number of winners approaches n, where n is the numbers of RSVP's and n is sufficiently large.)
     */
    println "...and the raffle winner is ${winner}!"
    
    // since we're not allowing dups, make sure we still have names left in the hat to draw from
    if (entryList.size() <= 0) { println "looks like everyone's a winner today! Thanks for playing!" } 
    else
    {
        if (input.readLine( 'Another drawing for this event? [y/n]: ') == "y")
         { getWinner(entryList) }
    }
}

// user-specific API key...hey! Get your own!
def API_KEY = '2a4d5328565aa4f3a2e2662674c6f10'
// top-level API url
def http = new HTTPBuilder( 'http://api.meetup.com' )
def input = System.in.newReader()

input.metaClass.readLine = { String prompt -> println prompt ; readLine() }

// first request, let's fetch all the events for the group with the url name "nashvillejug"
http.get( path: '/2/events' ,
    query: [
        key: API_KEY,
        sign: true,
        group_urlname: 'nashvillejug'
        ]) { eventsResponse, eventsJson ->
        
        def events = new JsonSlurper().parseText(eventsJson.toString())
        println "${events.meta.count} current event(s) going on..."
        
        def i = 1
        events.results.each() { println i++ + ": " + it.name } //print each event
        i = 0
        i = input.readLine("What is the number of the event you are looking for? 0 if none of these") //let the user choose which event they want
        if (i > 0)
        {
            fetchEvent(events.results[i-1])//let's use the event they chose.
            return //I want this to stop the script. TODO: Need to test if it does.
        }
    }
//if we fall through to here, then there are no events currently going on, so we try to use past events
http.get( path: '/2/events',
            query: [
                key: API_KEY,
                sign: true,
                group_urlname: 'nashvillejug',
                status: 'past' //see? I told you.
            ]) { pastEventsResponse, pastEventsJson ->
        
                def pastEvents = new JsonSlurper().parseText(pastEventsJson.toString())
                println "${pastEvents.meta.count} previous event(s) found..."
                if (pastEvents.results.size() > 0)
                {
                    i = 1;
                    for ( e in pastEvents.results)
                     { 
                         println i +". " + e.name
                         i++
                     } 
                    i = Integer.tryParse(input.readLine('What is the number of the event you are looking for? 0 if none of these'))
                    if (i > 0) { fetchEvent(pastEvents.results[i-1]) }    
                }
             }


def fetchEvent = {event ->
        println name
        println "${rsvpcount} people RSVP'd to attend the most recent event..."

        http.get( path: '/2/rsvps',
                query: [
                        key: API_KEY,
                        sign: true,
                        rsvp: 'yes',
                        event_id: id
                ]) { rsvpResponse, rsvpJson ->
// ok, so we have our event... let's see how many faithful JUG members RSVP'd to enter
            def rsvps = new JsonSlurper().parseText(rsvpJson.toString())
            rsvps.results.each {
                // and the final contestants are *drumroll*
                print "${it.member.name}, "
            }

            // OK, Johnny, tell us who won!
            getWinner(rsvps.results)
        }
}