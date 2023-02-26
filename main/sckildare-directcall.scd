(
s = Server.default;
s.waitForBoot { Routine {
	~stuff.free;
	~stuff = Engine_Kildare.new(s);
	n = NetAddr.localAddr;
	OSCFunc(
		{
			arg msg, time, addr, recvPort;
			var l = List.new;
			if(msg[1] != \load_file,{
				msg.removeAt(0);
				l.add(("/command/" ++ msg.removeAt(0)).asSymbol);
				l = l ++ msg;
				n.sendMsg(*l);
			},{
				msg[2] = msg[2].asInteger.asSymbol;
				msg[3] = msg[3].asString.replace("/home/we/","/Volumes/").asSymbol;
				msg.postln;
				msg.removeAt(0);
				l.add(("/command/" ++ msg.removeAt(0)).asSymbol);
				l = l ++ msg;
				n.sendMsg(*l);
				// ('sample load').postln;
			});
		},
		'/command'
	);
}.play; }
)