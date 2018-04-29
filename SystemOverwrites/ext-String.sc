+ String {
  prPostln { _PostLine }
	prPost { _PostString }
	postln {
		this.prPostln;
		defer { PostView.postln(this); PostViewNew.postln(this) };
	}
	post {
		this.prPost;
		defer { PostView.post(this); PostViewNew.postln(this) };
	}
}
