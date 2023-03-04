(
s = Server.default;
s.waitForBoot { Routine {
	n = NetAddr.localAddr;
	OSCFunc(
		{
			arg msg, time, addr, recvPort;
			var l = List.new;
			case
			{msg[1] == \cleanup} {
				~kildare.free;
			}
			{msg[1] == \establish_engine} {
				~kildare = Engine_Kildare.new(s);
			}
			{msg[1] != \load_file} {
				msg.removeAt(0);
				l.add(("/command/" ++ msg.removeAt(0)).asSymbol);
				l = l ++ msg;
				n.sendMsg(*l);
			}
			{msg[1] == \load_file} {
				msg[2] = msg[2].asInteger.asSymbol;
				msg[3] = msg[3].asString.replace("/home/we/","/Volumes/").asSymbol;
				msg.postln;
				msg.removeAt(0);
				l.add(("/command/" ++ msg.removeAt(0)).asSymbol);
				l = l ++ msg;
				n.sendMsg(*l);
			};
		},
		'/command'
	);
}.play; }
)