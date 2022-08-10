KildareCB {

	var <params;
	var <controlspecs;
	var <voiceGroup;

	*new { | out |
		^super.new.init(out)
	}

	init { | out |

		var s = Server.default;

		SynthDef(\kildare_cb, {
			arg out, stopGate = 1,
			amp, carHz, carDetune,
			modHz, modAmp, modAtk, modRel, feedAmp,
			modFollow, modNum, modDenum,
			carAtk, carRel,
			snap,
			pan, rampDepth, rampDec, amDepth, amHz,
			eqHz, eqAmp, bitRate, bitCount,
			lpHz, hpHz, filterQ,
			lpAtk, lpRel, lpDepth,
			squishPitch, squishChunk;

			var car, mod, carEnv, modEnv, carRamp, feedMod, feedCar, ampMod,
			voice_1, voice_2, filterEnv, mainSend;

			amp = amp*0.6;
			eqHz = eqHz.lag3(0.1);
			lpHz = lpHz.lag3(0.1);
			hpHz = hpHz.lag3(0.1);

			carHz = carHz * (2.pow(carDetune/12));
			modHz = Select.kr(modFollow > 0, [modHz, carHz * (modNum / modDenum)]);

			filterQ = LinLin.kr(filterQ,0,100,2.0,0.001);
			feedAmp = LinLin.kr(feedAmp,0.0,1.0,1.0,3.0);
			eqAmp = LinLin.kr(eqAmp,-2.0,2.0,-10.0,10.0);
			rampDepth = LinLin.kr(rampDepth,0.0,1.0,0.0,2.0);
			amDepth = LinLin.kr(amDepth,0,1.0,0.0,2.0);
			snap = LinLin.kr(snap,0.0,1.0,0.0,10.0);

			modEnv = EnvGen.kr(Env.perc(modAtk, modRel), gate:stopGate);
			carRamp = EnvGen.kr(Env([600, 0.000001], [rampDec], curve: \lin));
			carEnv = EnvGen.kr(Env.perc(carAtk, carRel),gate: stopGate);
			filterEnv = EnvGen.kr(Env.perc(lpAtk, lpRel, 1),gate: stopGate);

			voice_1 = LFPulse.ar((carHz) + (carRamp*rampDepth)) * carEnv * amp;
			voice_2 = SinOscFB.ar((carHz*1.5085)+ (carRamp*rampDepth),feedAmp) * carEnv * amp;
			ampMod = SinOsc.ar(freq:amHz,mul:amDepth,add:1);
			voice_1 = (voice_1+(LPF.ar(Impulse.ar(0.003),16000,1)*snap)) * ampMod;
			voice_1 = (voice_1*0.33)+(voice_2*0.33);
			voice_1 = Squiz.ar(in:voice_1, pitchratio:squishPitch, zcperchunk:squishChunk, mul:1);
			voice_1 = Decimator.ar(voice_1,bitRate,bitCount,1.0);
			voice_1 = BPeakEQ.ar(in:voice_1,freq:eqHz,rq:1,db:eqAmp,mul:1);
			voice_1 = RLPF.ar(in:voice_1,freq:Clip.kr(lpHz + ((5*(lpHz * filterEnv)) * lpDepth), 20, 20000), rq: filterQ, mul:1);
			voice_1 = RHPF.ar(in:voice_1,freq:hpHz, rq: filterQ, mul:1);

			voice_1 = Compander.ar(in:voice_1,control:voice_1, thresh:0.3, slopeBelow:1, slopeAbove:0.1, clampTime:0.01, relaxTime:0.01);
			mainSend = Pan2.ar(voice_1,pan);
			mainSend = mainSend * amp;

			Out.ar(out, mainSend);

			FreeSelf.kr(Done.kr(carEnv) * Done.kr(modEnv));

		}).send;

		// build a list of our sound-shaping parameters, with default values
		// (see https://doc.sccode.org/Classes/Dictionary.html for more about Dictionaries):
		params = Dictionary.newFrom([
			\out,out,
			\poly,0,
			\amp,0.7,
			\carAtk,0,
			\carRel,0.15,
			\feedAmp,0,
			\snap,0,
			\rampDepth,0,
			\rampDec,4,
			\squishPitch,1,
			\squishChunk,1,
			\amDepth,0,
			\amHz,2698.8,
			\eqHz,12000,
			\eqAmp,0,
			\bitRate,24000,
			\bitCount,24,
			\lpHz,24000,
			\hpHz,20,
			\filterQ,50,
			\lpAtk,0,
			\lpRel,0.3,
			\lpDepth,0,
			\pan,0,
		]);

		controlspecs = Dictionary.newFrom([
			\00, Dictionary.newFrom([\poly, ControlSpec.new(minval: 0.0, maxval: 1.0, warp: 'lin', step: 1, default: 0)]),
			\01, Dictionary.newFrom([\amp, ControlSpec.new(minval: 0.0, maxval: 1.0, warp: 'lin', step: 0.0, default: 0.55)]),
			\02, Dictionary.newFrom([\carAtk, ControlSpec.new(minval: 0.001, maxval: 10, warp: 'exp', units: 's', default: 0)]),
			\03, Dictionary.newFrom([\carRel, ControlSpec.new(minval: 0.001, maxval: 10, warp: 'exp', units: 's', default: 0.15)]),
			\04, Dictionary.newFrom([\feedAmp, ControlSpec.new(minval: 0, maxval: 1, warp: 'lin', default: 0)]),
			\05, Dictionary.newFrom([\snap, ControlSpec.new(minval: 0, maxval: 1, warp: 'lin', default: 0)]),
			\06, Dictionary.newFrom([\rampDepth, ControlSpec.new(minval: 0, maxval: 1, warp: 'lin', default: 0)]),
			\07, Dictionary.newFrom([\rampDec, ControlSpec.new(minval: 0.001, maxval: 10, warp: 'exp', units: 's', default: 4)]),
			\08, Dictionary.newFrom([\squishPitch, ControlSpec.new(minval: 1, maxval: 10, warp: 'lin', default: 1)]),
			\09, Dictionary.newFrom([\squishChunk, ControlSpec.new(minval: 1, maxval: 10, warp: 'lin', default: 1)]),
			\10, Dictionary.newFrom([\amDepth, ControlSpec.new(minval: 0, maxval: 1, warp: 'lin', default: 0)]),
			\11, Dictionary.newFrom([\amHz, ControlSpec.new(minval: 0.001, maxval: 12000, warp: 'exp', units: 'hz', default: 2698.8)]),
			\12, Dictionary.newFrom([\eqHz, ControlSpec.new(minval: 20, maxval: 20000, warp: 'exp', units: 'hz', default: 6000)]),
			\13, Dictionary.newFrom([\eqAmp, ControlSpec.new(minval: -2, maxval: 2, warp: 'lin', default: 0)]),
			\14, Dictionary.newFrom([\bitRate, ControlSpec.new(minval: 20, maxval: 24000, warp: 'exp', units: 'hz', default: 24000)]),
			\15, Dictionary.newFrom([\bitCount, ControlSpec.new(minval: 1, maxval: 24, warp: 'lin', units: 'bits', default: 24)]),
			\16, Dictionary.newFrom([\lpHz, ControlSpec.new(minval: 20, maxval: 20000, warp: 'exp', units: 'hz', default: 20000)]),
			\17, Dictionary.newFrom([\lpAtk, ControlSpec.new(minval: 0.001, maxval: 10, warp: 'exp', units: 's', default: 0.001)]),
			\18, Dictionary.newFrom([\lpRel, ControlSpec.new(minval: 0.001, maxval: 10, warp: 'exp', units: 's', default: 0.5)]),
			\19, Dictionary.newFrom([\lpDepth, ControlSpec.new(minval: 0, maxval: 1, warp: 'lin', default: 0)]),
			\20, Dictionary.newFrom([\hpHz, ControlSpec.new(minval: 20, maxval: 24000, warp: 'exp', units: 'hz', default: 20)]),
			\21, Dictionary.newFrom([\filterQ, ControlSpec.new(minval: 0, maxval: 100, warp: 'lin', units: '%', default: 50)]),
			\22, Dictionary.newFrom([\pan, ControlSpec.new(minval: -1, maxval: 1, warp: 'lin', default: 0)]),
		]);

	// NEW: register 'voiceGroup' as a Group on the Server
		voiceGroup = Group.new(s);
	}


	trigger {
		if( params[\poly] == 0,{
			voiceGroup.set(\stopGate, -1.1);
			Synth.new(\kildare_cb, params.getPairs, voiceGroup);
		},{
		// NEW: set the target of every Synth voice to the 'voiceGroup' Group
		Synth.new(\kildare_cb, params.getPairs, voiceGroup);
		});
	}

	setParam { arg paramKey, paramValue;
		// NEW: send changes to the paramKey, paramValue pair immediately to all voices
		if( params[\poly] == 0,{
			voiceGroup.set(paramKey, paramValue);
		});
		params[paramKey] = paramValue;
	}

	// NEW: free our Group when the class is freed
	free {
		voiceGroup.free;
	}

}