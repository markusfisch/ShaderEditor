import { defineCollection } from 'astro:content';
import { glob } from 'astro/loaders';
import { z } from 'astro/zod';
import { faqLoader } from './loaders/faq.ts';

const accent = z.enum(['cyan', 'magenta', 'violet', 'lime', 'orange']);
const icon = z.enum([
	'zap',
	'palette',
	'wallpaper',
	'camera',
	'layers',
	'code',
	'hand',
	'volume-2',
	'clock',
	'cpu',
	'settings',
	'download',
	'play',
	'chevron-down',
	'chevron-left',
	'chevron-right',
	'x',
	'copy',
	'external-link',
	'book-open',
	'file-code',
	'terminal',
	'keyboard',
	'alert-triangle',
	'library',
	'image',
	'sliders-horizontal',
	'sparkles',
]);

const walkthroughs = defineCollection({
	loader: glob({
		base: './src/content/walkthroughs',
		pattern: '**/*.md',
	}),
	schema: z.object({
		group: z.enum(['basics', 'input', 'camera', 'backbuffer', 'advanced']),
		order: z.number().int().positive(),
		name: z.string(),
		desc: z.string(),
		video: z.string().optional(),
		poster: z.string().optional(),
		links: z.array(z.object({
			label: z.string(),
			href: z.string().url(),
		})).default([]),
		code: z.string(),
	}),
});

const faq = defineCollection({
	loader: faqLoader(),
	schema: z.object({
		order: z.number().int().positive(),
		question: z.string(),
		questionHtml: z.string(),
	}),
});

const features = defineCollection({
	loader: glob({
		base: './src/content/features',
		pattern: '**/*.json',
	}),
	schema: z.object({
		order: z.number().int().positive(),
		icon,
		title: z.string(),
		desc: z.string(),
		detail: z.string(),
		media: z.string(),
		mediaType: z.enum(['video', 'image']),
		poster: z.string().optional(),
		accent,
	}),
});

const gallery = defineCollection({
	loader: glob({
		base: './src/content/gallery',
		pattern: '**/*.json',
	}),
	schema: z.object({
		group: z.enum(['editing', 'library', 'output']),
		order: z.number().int().positive(),
		title: z.string(),
		desc: z.string(),
		icon,
		accent,
		src: z.string(),
		type: z.enum(['image', 'video']),
		poster: z.string().optional(),
	}),
});

export const collections = {
	walkthroughs,
	faq,
	features,
	gallery,
};
