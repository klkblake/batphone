package org.servalproject.auth;

import java.util.Random;

import org.servalproject.R;
import org.servalproject.ServalBatPhoneApplication;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

public class AuthSymbols extends Activity {

	private static final int REQUEST = 1;
	public static final String EXTRA_SYMBOL_GENERATOR_INDEX = "org.servalproject.auth.symbol_generator";

	private static final int MIN_SYMBOLS = 4;
	private static final int MAX_SYMBOLS = 16;
	private static final double FAIL_THRESHOLD = 0.5;
	private static final double SUCCEED_THRESHOLD = 0.8;

	public enum State {
		YOU, THEM
	};

	private State state;

	private SymbolGenerator yours;
	private SymbolGenerator theirs;

	private TextView group;
	private TextView attackProbability;
	private TextView title;
	private FrameLayout symbolFrame;
	private TextView query;

	private int total = 0;
	private int yourMatches = 0;
	private int theirMatches = 0;
	private int yourFailures = 0;
	private int theirFailures = 0;
	private double probability = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.auth_symbol);

		int index = getIntent().getIntExtra(EXTRA_SYMBOL_GENERATOR_INDEX, -1);
		if (index == -1) {
			throw new IllegalArgumentException(EXTRA_SYMBOL_GENERATOR_INDEX
					+ " not set");
		}
		SymbolGeneratorFactory sgf = SymbolGenerators.get()[index];
		Random rand = new Random(1234);
		if (ServalBatPhoneApplication.context.callHandler.initiated) {
			state = State.YOU;
			yours = sgf.getSymbolGenerator(new Random(rand.nextLong()));
			theirs = sgf.getSymbolGenerator(new Random(rand.nextLong()));
		} else {
			state = State.THEM;
			theirs = sgf.getSymbolGenerator(new Random(rand.nextLong()));
			yours = sgf.getSymbolGenerator(new Random(rand.nextLong()));
		}

		group = (TextView) findViewById(R.id.auth_symbol_num);
		attackProbability = (TextView) findViewById(R.id.auth_attack_probability);
		title = (TextView) findViewById(R.id.auth_symbol_title);
		symbolFrame = (FrameLayout) findViewById(R.id.auth_symbol);
		query = (TextView) findViewById(R.id.auth_query);

		next();

		Button cancel = (Button) findViewById(R.id.auth_cancel_button);
		cancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				setResult(AuthResult.CANCELLED);
				finish();
			}
		});

		Button yes = (Button) findViewById(R.id.auth_yes_button);
		yes.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				switch (state) {
				case YOU:
					yourMatches++;
					break;
				case THEM:
					theirMatches++;
					break;
				}
				eval();
			}
		});

		Button no = (Button) findViewById(R.id.auth_no_button);
		no.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				switch (state) {
				case YOU:
					yourFailures++;
					break;
				case THEM:
					theirFailures++;
					break;
				}
				eval();
			}
		});
	}

	// XXX This does not currently take into account how much entropy is in each
	// symbol.
	private void eval() {
		int yourTotal = yourMatches + yourFailures;
		int theirTotal = theirMatches + theirFailures;
		double yourRatio = (double) yourMatches / (double) yourTotal;
		double theirRatio = (double) theirMatches / (double) theirTotal;
		double yourAdjustedRatio = (double) (yourMatches + 1)
				/ (double) (yourTotal + 2);
		double theirAdjustedRatio = (double) (theirMatches + 1)
				/ (double) (theirTotal + 2);
		// This is not yet an actual probability, but it should at least
		// look vaguely like one.
		probability = 1 - (yourAdjustedRatio + theirAdjustedRatio) / 2;
		if (yourTotal < MIN_SYMBOLS || theirTotal < MIN_SYMBOLS) {
			switchState();
			next();
			return;
		}
		if (yourTotal > MAX_SYMBOLS || theirTotal > MAX_SYMBOLS) {
			fail();
			return;
		}
		if (yourRatio < FAIL_THRESHOLD || theirRatio < FAIL_THRESHOLD) {
			fail();
			return;
		}
		if (yourRatio > SUCCEED_THRESHOLD && theirRatio > SUCCEED_THRESHOLD) {
			succeed();
			return;
		}
		if (yourRatio > SUCCEED_THRESHOLD) {
			state = State.THEM;
			next();
			return;
		}
		if (theirRatio > SUCCEED_THRESHOLD) {
			state = State.YOU;
			next();
			return;
		}
		switchState();
		next();
	}

	private void succeed() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Auth Succeeded");
		builder.show();
		setResult(AuthResult.SUCCESS);
		finish();
	}

	private void fail() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Auth Failed");
		builder.show();
		setResult(AuthResult.FAILURE);
		finish();
	}

	private void switchState() {
		switch (state) {
		case YOU:
			state = State.THEM;
			break;
		case THEM:
			state = State.YOU;
			break;
		}
	}

	private void next() {
		total++;
		group.setText(getString(R.string.auth_symbol_num, total));
		attackProbability.setText(getString(R.string.auth_attack_probability,
				probability));
		symbolFrame.removeAllViews();
		switch (state) {
		case YOU:
			title.setText(R.string.auth_your_symbol_title);
			symbolFrame.addView(yours.getSymbolBlock(this));

			query.setText(R.string.auth_query_yours);
			break;
		case THEM:
			title.setText(R.string.auth_their_symbol_title);
			symbolFrame.addView(theirs.getSymbolBlock(this));
			query.setText(R.string.auth_query_theirs);
			break;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST && resultCode != AuthResult.BACK) {
			setResult(resultCode);
			finish();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		finishActivity(REQUEST);
	}

}
