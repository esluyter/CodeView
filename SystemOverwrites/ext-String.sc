+ String {
  prPostln { _PostLine }
	prPost { _PostString }
	postln {
		this.prPostln;
		defer { PostView.postln(this); };
	}
	post {
		this.prPost;
		defer { PostView.post(this); };
	}
}
