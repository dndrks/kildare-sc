(
s = Server.default;
s.waitForBoot { Routine {

	n = NetAddr.localAddr;

	OSCFunc(
		{
			arg msg, time, addr, recvPort;
			var l = List.new;
			msg[2] = msg[2].asInteger.asSymbol;
			msg[3] = msg[3].asString.replace("/home/we/","/Volumes/").asSymbol;
			msg.postln;
			msg.removeAt(0);
			l.add(("/command/" ++ msg.removeAt(0)).asSymbol);
			l = l ++ msg;
			n.sendMsg(*l);
		},
		'/load_file_from_norns'
	);

}.play;}
)

// 0 is for delay return
n.sendMsg(*[ "/command/set_voice_param", "1", "out","2"]);
n.sendMsg(*[ "/command/set_voice_param", "2", "out","4"]);
n.sendMsg(*[ "/command/set_voice_param", "3", "out","6"]);
n.sendMsg(*[ "/command/set_voice_param", "4", "out","8"]);
n.sendMsg(*[ "/command/set_voice_param", "5", "out","10"]);
n.sendMsg(*[ "/command/set_voice_param", "6", "out","12"]);
n.sendMsg(*[ "/command/set_voice_param", "7", "out","14"]);

n.sendMsg('/engine/load/name', 'None');