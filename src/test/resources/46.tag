tree subj:
	S[] {
		N+
		VP! @NA
	}

word 'mer': subj

tree copy_a:
	VP @NA {
		A+
		VP {
			A_leaf!
		}
	}

word 'a': copy_a

tree just_a:
	A_leaf+

word 'a': just_a

tree copy_b:
	VP @NA {
		B+
		VP {
			B_leaf!
		}
	}

word 'b': copy_b

tree just_b:
	B_leaf+

word 'b': just_b

tree copy_c:
	VP @NA {
		C+
		VP {
			C_leaf!
		}
	}

word 'c': copy_c

tree just_c:
	C_leaf+

word 'c': just_c

tree aux_a:
	VP @NA {
		A+
		VP {
			VP*
			VP @NA {
				A_leaf!
			}
		}
	}

word 'a': aux_a

tree aux_b:
	VP @NA {
		B+
		VP {
			VP*
			VP @NA {
				B_leaf!
			}
		}
	}

word 'b': aux_b

tree aux_c:
	VP @NA {
		C+
		VP {
			VP*
			VP @NA {
				C_leaf!
			}
		}
	}

word 'c': aux_c



