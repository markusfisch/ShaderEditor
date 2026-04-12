import { readFile } from 'node:fs/promises';
import type { Loader } from 'astro/loaders';

interface FaqSection {
	question: string;
	body: string;
}

function slugify(value: string) {
	return value
		.toLowerCase()
		.normalize('NFKD')
		.replace(/[\u0300-\u036f]/g, '')
		.replace(/[^a-z0-9]+/g, '-')
		.replace(/(^-|-$)/g, '');
}

function splitFaq(markdown: string) {
	const lines = markdown.replace(/\r\n?/g, '\n').split('\n');
	const sections: FaqSection[] = [];
	let question = '';
	let body: string[] = [];
	let inFence = false;

	const pushSection = () => {
		if (!question) {
			return;
		}
		sections.push({
			question,
			body: body.join('\n').trim(),
		});
	};

	for (const line of lines) {
		if (/^```/.test(line.trim())) {
			inFence = !inFence;
		}

		const heading = !inFence && line.match(/^##\s+(.*?)\s*#*\s*$/);
		if (heading) {
			pushSection();
			question = heading[1];
			body = [];
			continue;
		}

		if (question) {
			body.push(line);
		}
	}

	pushSection();
	return sections;
}

export function faqLoader() {
	return {
		name: 'faq-loader',
		async load({ config, renderMarkdown, store }) {
			const fileURL = new URL('../FAQ.md', config.root);
			const markdown = await readFile(fileURL, 'utf8');
			const sections = splitFaq(markdown);

			store.clear();

			for (const [index, section] of sections.entries()) {
				const order = index + 1;
				const id = `${String(order).padStart(2, '0')}-${slugify(section.question)}`;
				const questionHtml = (await renderMarkdown(section.question, {
					fileURL,
				})).html
					.trim()
					.replace(/^<h[1-6][^>]*>/, '')
					.replace(/^<p>/, '')
					.replace(/<\/h[1-6]>$/, '')
					.replace(/<\/p>$/, '');

				store.set({
					id,
					data: {
						order,
						question: section.question,
						questionHtml,
					},
					rendered: await renderMarkdown(section.body, {
						fileURL,
					}),
				});
			}
		},
	} satisfies Loader;
}
