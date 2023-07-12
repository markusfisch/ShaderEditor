# Contribution Guidelines

Please try to keep things in good shape and comply to what's there.

This project follows Android [best practices][android_best_practices]
so please have a look if you've never heard of them.

The code is formatted according to Android Studio's standard, with the
exception of indent being tabs instead of spaces and all alignment disabled.

If you enable `EditorConfig` support _(disabled by default, go to
`Settings` → `Editor` → `Code Style` → `Enable EditorConfig support`)_,
the `.editorconfig` file will automatically override your settings.

Use the feature branch workflow to add new features and make sure
to squash when merging into master:

	$ git merge cool_feature --squash

Then write a [good commit message][commit_messages] to keep the history
meaningful and useful. One feature, one commit.

[android_best_practices]: https://developer.android.com/distribute/best-practices/develop/
[commit_messages]: https://git-scm.com/book/en/v2/Distributed-Git-Contributing-to-a-Project#_commit_guidelines
