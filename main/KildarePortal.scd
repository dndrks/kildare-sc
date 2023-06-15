(
s = Server.default;
s.waitForBoot { Routine {
	Server.default.options.outDevice_("BlackHole 16ch");
	s.options.numOutputBusChannels = 16;
	s.options.numWireBufs = 128;
	s.reboot;

	5.wait;

	~voices = Dictionary.new();

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
			{msg[1] == \pset_load} {
				~kildare.free;
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

	('initial Kildare structure loaded!').postln;

}.play;}
)

// n.sendMsg("/command", "establish_engine")
// n.sendMsg(*[ "/command/init_voice", "1", "kildare_bd"])
// n.sendMsg(*[ "/command/trig", 1.0, 127.0, true, 1.0 ])
n.sendMsg(*[ "/command/set_voice_param", "1", "out","0"]);
n.sendMsg(*[ "/command/set_voice_param", "2", "out","4"]);
n.sendMsg(*[ "/command/set_voice_param", "3", "out","6"]);
n.sendMsg(*[ "/command/set_voice_param", "4", "out","8"]);
n.sendMsg(*[ "/command/set_voice_param", "5", "out","10"]);
n.sendMsg(*[ "/command/set_voice_param", "6", "out","12"]);
n.sendMsg(*[ "/command/set_voice_param", "7", "out","14"]);